package com.ccomp.br.domain.clubs.materials.application;

import com.ccomp.br.domain.clubs.members.application.ClubAccessPolicy;
import com.ccomp.br.domain.clubs.core.persistence.Club;
import com.ccomp.br.domain.clubs.core.persistence.ClubRepository;
import com.ccomp.br.domain.clubs.materials.dto.CreateClubMaterialLinkRequest;
import com.ccomp.br.domain.clubs.materials.dto.ClubMaterialCursor;
import com.ccomp.br.domain.clubs.materials.dto.ClubMaterialResponse;
import com.ccomp.br.domain.clubs.materials.enums.EnumClubMaterialType;
import com.ccomp.br.domain.clubs.materials.persistence.ClubMaterial;
import com.ccomp.br.domain.clubs.materials.persistence.ClubMaterialDslRepository;
import com.ccomp.br.domain.clubs.materials.persistence.ClubMaterialRepository;
import com.ccomp.br.domain.clubs.materials.utils.ClubMaterialMapper;
import com.ccomp.br.domain.storage.external.StorageExternal;
import com.ccomp.br.domain.storage.external.StorageKeyGenerator;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.utils.CursorPage;
import com.ccomp.br.shared.utils.CursorUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class ClubMaterialsService {
    private static final Logger log = LoggerFactory.getLogger(ClubMaterialsService.class);
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final List<String> ALLOWED_TYPES = List.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "image/webp");
    private final ClubRepository clubRepository;
    private final ClubMaterialRepository materialRepository;
    private final ClubMaterialDslRepository materialDslRepository;
    private final ClubAccessPolicy accessPolicy;
    private final StorageExternal storage;
    private final StorageKeyGenerator keyGenerator;
    private final ClubMaterialMapper mapper;
    private final Clock clock;

    public ClubMaterialsService(ClubRepository clubRepository, ClubMaterialRepository materialRepository,
                                 ClubAccessPolicy accessPolicy,
                                 StorageExternal storage,
                                 ClubMaterialDslRepository materialDslRepository,
                                 StorageKeyGenerator keyGenerator,
                                 ClubMaterialMapper mapper, Clock clock) {
        this.clubRepository = clubRepository;
        this.materialRepository = materialRepository;
        this.accessPolicy = accessPolicy;
        this.storage = storage;
        this.materialDslRepository = materialDslRepository;
        this.keyGenerator = keyGenerator;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CursorPage<ClubMaterialResponse> list(
            Long clubId, @Nullable UUID userId, String cursorRaw, int pageSize) {
        Club club = findClub(clubId);
        assertCanView(club, userId);

        int limit = Math.min(pageSize, 50);
        ClubMaterialCursor cursor = CursorUtils.decode(cursorRaw, ClubMaterialCursor.class);
        List<ClubMaterial> results = materialDslRepository.findAllByClubIdWithCursor(
                clubId, cursor, limit + 1);
        return CursorUtils.buildPage(results, limit, mapper::toDTO,
                material -> new ClubMaterialCursor(material.getCreatedAt(), material.getId()));
    }

    @Transactional
    public ClubMaterialResponse addLink(Long clubId, UUID userId, CreateClubMaterialLinkRequest request) {
        findClub(clubId);
        assertCanEdit(clubId, userId);
        String url = validateUrl(request.url());
        ClubMaterial material = ClubMaterial.builder()
                .clubId(clubId)
                .createdBy(userId)
                .createdAt(LocalDateTime.now(clock))
                .title(request.title().trim())
                .description(request.description())
                .type(EnumClubMaterialType.LINK)
                .url(url)
                .build();
        return mapper.toDTO(materialRepository.save(material));
    }

    @Transactional
    public ClubMaterialResponse upload(Long clubId, UUID userId, String title, String description, MultipartFile file) {
        findClub(clubId);
        assertCanEdit(clubId, userId);

        validateFile(title, description, file);

        String contentType = file.getContentType();
        validateFileSignature(contentType, file);

        String key = keyGenerator.generate(
                "clubs/%d/materials".formatted(clubId),
                "material." + extensionFor(contentType));
        storage.putObject(key, file);
        registerStorageCleanupOnRollback(key);
        ClubMaterial material = ClubMaterial.builder()
                .clubId(clubId)
                .createdBy(userId)
                .createdAt(LocalDateTime.now(clock))
                .title(title.trim())
                .description(description)
                .type(contentType.startsWith("image/") ? EnumClubMaterialType.IMAGE : EnumClubMaterialType.FILE)
                .fileKey(key)
                .originalFileName(safeFileName(file.getOriginalFilename()))
                .contentType(contentType)
                .sizeBytes(file.getSize())
                .build();
        return mapper.toDTO(materialRepository.save(material));
    }

    @Transactional(readOnly = true)
    public Optional<StorageExternal.StoredFile> getFile(Long clubId, Long materialId, @Nullable UUID userId) {
        Club club = findClub(clubId);
        assertCanView(club, userId);
        ClubMaterial material = findMaterial(clubId, materialId);
        if (material.getType() == EnumClubMaterialType.LINK) return Optional.empty();
        return storage.getObject(material.getFileKey());
    }

    @Transactional
    public void delete(Long clubId, Long materialId, UUID userId) {
        findClub(clubId);
        assertCanEdit(clubId, userId);
        ClubMaterial material = findMaterial(clubId, materialId);
        materialRepository.delete(material);
        if (material.getType() != EnumClubMaterialType.LINK) {
            registerStorageDeletionAfterCommit(material.getFileKey());
        }
    }

    private void registerStorageCleanupOnRollback(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    deleteStorageObject(key, "compensar upload revertido");
                }
            }
        });
    }

    private void registerStorageDeletionAfterCommit(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteStorageObject(key, "excluir material removido");
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteStorageObject(key, "excluir material removido");
            }
        });
    }

    private void deleteStorageObject(String key, String operation) {
        try {
            storage.deleteObject(key);
        } catch (RuntimeException e) {
            log.error("Falha ao {} no storage: {}", operation, key, e);
        }
    }

    private Club findClub(Long id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clube não encontrado."));
    }

    private void assertCanView(Club club, @Nullable UUID userId) {
        boolean canView = accessPolicy.isMember(club.getId(), userId)
                || accessPolicy.isManager(club.getId(), userId);
        if (!canView) {
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");
        }
    }

    private void assertCanEdit(Long clubId, UUID userId) {
        if (!accessPolicy.isManager(clubId, userId)) {
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");
        }
    }

    private ClubMaterial findMaterial(Long clubId, Long id) {
        return materialRepository.findByIdAndClubId(id, clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Material não encontrado."));
    }

    private String validateUrl(String value) {
        try {
            URI uri = URI.create(value.trim());
            if (!uri.isAbsolute()
                    || uri.getHost() == null
                    || !(uri.getScheme().equalsIgnoreCase("http")
                    || uri.getScheme().equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException();
            }
            return uri.toASCIIString();
        } catch (RuntimeException e) {
            throw new DomainException("Informe um link válido com protocolo HTTP ou HTTPS.");
        }
    }

    private void validateFile(String title, String description, MultipartFile file) {
        if (title == null || title.isBlank()) {
            throw new DomainException("O título do material é obrigatório.");
        }
        if (title.length() > 255) {
            throw new DomainException("O título deve ter no máximo 255 caracteres.");
        }
        if (description != null && description.length() > 1000) {
            throw new DomainException("A descrição deve ter no máximo 1000 caracteres.");
        }
        if (file == null || file.isEmpty()) {
            throw new DomainException("Nenhum arquivo enviado.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new DomainException("O arquivo excede o limite de 10 MB.");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new DomainException("Tipo não suportado. Utilize PDF, PNG, JPEG ou WebP.");
        }
    }

    private String safeFileName(String name) {
        return name == null
                ? "arquivo"
                : name.replaceAll("[\\r\\n\\\\/]", "_");
    }

    private String extensionFor(String type) {
        return switch (type) {
            case "application/pdf" -> "pdf";
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            default -> "webp";
        };
    }

    private void validateFileSignature(String contentType, MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            boolean valid = switch (contentType) {
                case "application/pdf" -> startsWith(bytes, new byte[]{'%', 'P', 'D', 'F', '-'});
                case "image/png" -> startsWith(
                        bytes, new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10});
                case "image/jpeg" -> startsWith(
                        bytes, new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff});
                case "image/webp" -> bytes.length >= 12
                        && startsWith(bytes, new byte[]{'R', 'I', 'F', 'F'})
                        && bytes[8] == 'W'
                        && bytes[9] == 'E'
                        && bytes[10] == 'B'
                        && bytes[11] == 'P';
                default -> false;
            };
            if (!valid) {
                throw new DomainException("O conteúdo do arquivo não corresponde ao tipo informado.");
            }
        } catch (IOException e) {
            throw new DomainException("Não foi possível ler o arquivo enviado.");
        }
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (value[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
