package com.ccomp.br.module.email;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class EmailTemplateService {
    public String render(String templateName, String title, Map<String, String> values) {
        String content = readTemplate(templateName);
        for (Map.Entry<String, String> value : values.entrySet()) {
            content = content.replace("{{" + value.getKey() + "}}", escapeHtml(value.getValue()));
        }

        String layout = readTemplate("layout");
        layout = layout.replace("{{title}}", escapeHtml(title));
        return layout.replace("{{body}}", content);
    }

    private String readTemplate(String name) {
        try (var input = new ClassPathResource("templates/email/" + name + ".html").getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível carregar o modelo de e-mail.", e);
        }
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
