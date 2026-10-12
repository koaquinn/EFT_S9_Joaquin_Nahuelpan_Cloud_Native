package cl.duoc.dsy2207.usuarios.graphql;

import cl.duoc.dsy2207.usuarios.eventos.PublicadorEventos;
import cl.duoc.dsy2207.usuarios.model.Usuario;
import cl.duoc.dsy2207.usuarios.repository.UsuarioRepository;
import graphql.GraphQL;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.util.Optional;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;

public final class UsuariosGraphQL {
    private static final String SDL = """
            type Usuario {
                id: ID
                nombre: String
                username: String
                email: String
                activo: Boolean
                rolId: Int
            }

            type Query {
                usuarios: [Usuario]
                usuario(id: ID!): Usuario
            }

            type Mutation {
                crearUsuario(nombre: String!, username: String!, email: String!, activo: Boolean, rolId: Int): Usuario
                actualizarUsuario(id: ID!, nombre: String!, username: String!, email: String!, activo: Boolean, rolId: Int): Usuario
                eliminarUsuario(id: ID!): Boolean
            }
            """;

    private static final GraphQL INSTANCIA = construir();

    private UsuariosGraphQL() {
    }

    public static GraphQL instancia() {
        return INSTANCIA;
    }

    private static GraphQL construir() {
        UsuarioRepository repositorio = new UsuarioRepository();

        TypeDefinitionRegistry registro = new SchemaParser().parse(SDL);

        RuntimeWiring wiring = newRuntimeWiring()
                .type("Query", builder -> builder
                        .dataFetcher("usuarios", env -> repositorio.listar())
                        .dataFetcher("usuario", env ->
                                repositorio.buscarPorId(Long.parseLong(env.getArgument("id"))).orElse(null)))
                .type("Mutation", builder -> builder
                        .dataFetcher("crearUsuario", env -> {
                            Usuario creado = repositorio.crear(desdeArgumentos(env));
                            PublicadorEventos.usuarioCreado(creado);
                            return creado;
                        })
                        .dataFetcher("actualizarUsuario", env -> {
                            long id = Long.parseLong(env.getArgument("id"));
                            Usuario usuario = desdeArgumentos(env);
                            if (!repositorio.actualizar(id, usuario)) {
                                return null;
                            }
                            usuario.setId(id);
                            PublicadorEventos.usuarioActualizado(usuario);
                            return usuario;
                        })
                        .dataFetcher("eliminarUsuario", env -> {
                            long id = Long.parseLong(env.getArgument("id"));
                            Optional<Usuario> previo = repositorio.buscarPorId(id);
                            if (previo.isEmpty() || !repositorio.eliminar(id)) {
                                return false;
                            }
                            PublicadorEventos.usuarioEliminado(previo.get());
                            return true;
                        }))
                .build();

        GraphQLSchema esquema = new SchemaGenerator().makeExecutableSchema(registro, wiring);
        return GraphQL.newGraphQL(esquema).build();
    }

    private static Usuario desdeArgumentos(DataFetchingEnvironment env) {
        Usuario usuario = new Usuario();
        usuario.setNombre(env.getArgument("nombre"));
        usuario.setUsername(env.getArgument("username"));
        usuario.setEmail(env.getArgument("email"));
        usuario.setActivo(env.getArgument("activo"));
        Integer rolId = env.getArgument("rolId");
        usuario.setRolId(rolId == null ? null : rolId.longValue());
        return usuario;
    }
}
