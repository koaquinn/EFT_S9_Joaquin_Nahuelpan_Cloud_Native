package cl.duoc.dsy2207.usuarios;

import cl.duoc.dsy2207.usuarios.graphql.UsuariosGraphQL;
import cl.duoc.dsy2207.usuarios.util.JsonUtil;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import graphql.ExecutionInput;
import graphql.ExecutionResult;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class UsuariosGraphQLFunction {
    @FunctionName("usuariosGraphQL")
    public HttpResponseMessage graphql(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS, route = "graphql")
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        try {
            Optional<String> cuerpo = request.getBody();
            if (cuerpo.isEmpty() || cuerpo.get().isBlank()) {
                return json(request, HttpStatus.BAD_REQUEST,
                        Map.of("errors", "El cuerpo debe contener una consulta GraphQL"));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> peticion = JsonUtil.fromJson(cuerpo.get(), Map.class);
            Object consulta = peticion.get("query");
            if (consulta == null || consulta.toString().isBlank()) {
                return json(request, HttpStatus.BAD_REQUEST,
                        Map.of("errors", "Falta el campo 'query'"));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> variables = peticion.get("variables") instanceof Map
                    ? (Map<String, Object>) peticion.get("variables")
                    : Collections.emptyMap();

            ExecutionInput entrada = ExecutionInput.newExecutionInput()
                    .query(consulta.toString())
                    .operationName((String) peticion.get("operationName"))
                    .variables(variables)
                    .build();

            ExecutionResult resultado = UsuariosGraphQL.instancia().execute(entrada);
            return json(request, HttpStatus.OK, resultado.toSpecification());
        } catch (IllegalArgumentException e) {
            return json(request, HttpStatus.BAD_REQUEST, Map.of("errors", e.getMessage()));
        } catch (Exception e) {
            context.getLogger().severe("Error en GraphQL de usuarios: " + e.getMessage());
            return json(request, HttpStatus.INTERNAL_SERVER_ERROR,
                    Map.of("errors", "Error interno: " + e.getMessage()));
        }
    }

    private HttpResponseMessage json(HttpRequestMessage<Optional<String>> request, HttpStatus status, Object cuerpo) {
        return request.createResponseBuilder(status)
                .header("Content-Type", "application/json; charset=utf-8")
                .body(JsonUtil.toJson(cuerpo))
                .build();
    }
}
