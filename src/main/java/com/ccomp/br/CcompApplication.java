package com.ccomp.br;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/*
* GERAR CAHVES:
* - privada: openssl genpkey -algorithm RSA -out private.key -pkeyopt rsa_keygen_bits:2048
* - publica: openssl rsa -pubout -in private.key -out public.key
* E colocar em resources.
 * */

// - Criar um mapper, ou usar um, para centralizar as converções.
// - Criar um refrest token em auth/.
// - Criar usuarios admin e roles.
// - Terminar as tabelas do banco.
// - Integrar com email e whatsapp. Obs: Colocar a lógica nos modulos.
// - Conectar a um banco postgree usando dados do .env.
// - Paginação é necessario.
@SpringBootApplication
@RestController
public class CcompApplication {

	public static void main(String[] args) {
		SpringApplication.run(CcompApplication.class, args);
	}

    @GetMapping
    public String ping(){
        return "OK!";
    }
}
