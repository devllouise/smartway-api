package com.smartway.api.adapter.in.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RegisterRequest {
    @NotBlank(message = "Nome e obrigatorio")
    private String name;

    @NotBlank(message = "Email e obrigatorio")
    @Email(message = "Email invalido")
    private String email;

    @NotBlank(message = "Senha e obrigatoria")
    @Size(min = 6, message = "Senha deve ter no minimo 6 caracteres")
    private String password;

    /** Respostas da triagem. Se vier nulo (usuario pulou), o perfil e EQUILIBRADO. */
    private Profiling profiling;


    @Data
    public static class Profiling {
        private List<String> priorities;
        private List<String> modes;
    }
}
