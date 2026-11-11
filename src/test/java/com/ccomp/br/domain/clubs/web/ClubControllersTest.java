package com.ccomp.br.domain.clubs.web;

import com.ccomp.br.domain.clubs.application.ClubMemberService;
import com.ccomp.br.domain.clubs.application.ClubService;
import com.ccomp.br.domain.clubs.dto.ClubMemberFilter;
import com.ccomp.br.domain.clubs.dto.ClubMemberListItem;
import com.ccomp.br.domain.clubs.dto.ClubResponseDTO;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberRole;
import com.ccomp.br.domain.clubs.enums.EnumClubMemberStatus;
import com.ccomp.br.domain.clubs.persistence.members.ClubMember;
import com.ccomp.br.shared.utils.CursorPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ClubControllersTest {
    private static final UUID USER_ID = UUID.fromString("b7fc1e4f-0492-4da8-b420-2d010ae5350b");
    @Mock ClubService clubService;
    @Mock ClubMemberService memberService;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        var jwt = Jwt.withTokenValue("test").header("alg", "none").subject(USER_ID.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(jwt, null, List.of()));
        mvc = MockMvcBuilders.standaloneSetup(new ClubController(clubService), new ClubMemberController(memberService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
    }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    @Test
    void publicSearchUsesDefaultPageSize() throws Exception {
        when(clubService.search(null, 10)).thenReturn(new CursorPage<>(List.<ClubResponseDTO>of(), null, null));
        mvc.perform(get("/api/clubs/search")).andExpect(status().isOk());
        verify(clubService).search(null, 10);
    }

    @Test
    void memberSearchMapsFilterAndPaginationParameters() throws Exception {
        when(memberService.searchMembers(eq(USER_ID), eq(4L), any(ClubMemberFilter.class), eq("cursor"), eq(7)))
                .thenReturn(new CursorPage<>(List.<ClubMemberListItem>of(), null, null));
        mvc.perform(post("/api/clubs/4/members/search?cursor=cursor&size=7")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"MEMBER\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
        verify(memberService).searchMembers(eq(USER_ID), eq(4L),
                eq(new ClubMemberFilter(EnumClubMemberRole.MEMBER, EnumClubMemberStatus.ACTIVE)), eq("cursor"), eq(7));
    }

    @Test
    void enrollForwardsAuthenticatedUser() throws Exception {
        when(memberService.enrollMember(4L, USER_ID)).thenReturn(ClubMember.builder().userId(USER_ID).build());
        mvc.perform(post("/api/clubs/4/members/enroll")).andExpect(status().isOk());
        verify(memberService).enrollMember(4L, USER_ID);
    }
}
