package com.ccomp.br.config.dev;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@Profile("!prod")
public class DependencyTreePrinter implements CommandLineRunner {

    @Autowired
    private ApplicationContext applicationContext;

    private static final String SEU_PACOTE_BASE = "com.ccomp";

    @Override
    public void run(String... args) {
        System.out.println("\n==========================================");
        System.out.println("   ÁRVORE DE INJEÇÃO DE DEPENDÊNCIAS");
        System.out.println("==========================================");

        Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(RestController.class);

        for (Object bean : controllers.values()) {
            Class<?> clazz = AopUtils.getTargetClass(bean);
            System.out.println("└── " + clazz.getSimpleName());
            printDependencies(clazz, "    │", new HashSet<>());
            System.out.println();
        }
    }

    private void printDependencies(Class<?> clazz, String indent, Set<Class<?>> visited) {
        if (visited.contains(clazz)) return;
        visited.add(clazz);

        for (Field field : clazz.getDeclaredFields()) {
            Class<?> fieldType = field.getType();

            // Mapeia apenas as classes do seu próprio código
            if (fieldType.getName().startsWith(SEU_PACOTE_BASE)) {
                System.out.println(indent + "└── " + fieldType.getSimpleName());
                printDependencies(fieldType, indent + "    │", visited);
            }
        }
    }
}
