package cl.duoc.dsy2207.roles.graphql;

import cl.duoc.dsy2207.roles.eventos.PublicadorEventos;
import cl.duoc.dsy2207.roles.model.Rol;
import cl.duoc.dsy2207.roles.repository.RolRepository;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.util.Optional;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;

public final class RolesGraphQL {
    private static final String SDL = """
            type Rol {
                id: ID
                nombre: String
                descripcion: String
            }

            type Query {
                roles: [Rol]
                rol(id: ID!): Rol
            }

            type Mutation {
                crearRol(nombre: String!, descripcion: String): Rol
                actualizarRol(id: ID!, nombre: String!, descripcion: String): Rol
                eliminarRol(id: ID!): Boolean
            }
            """;

    private static final GraphQL INSTANCIA = construir();

    private RolesGraphQL() {
    }

    public static GraphQL instancia() {
        return INSTANCIA;
    }

    private static GraphQL construir() {
        RolRepository repositorio = new RolRepository();

        TypeDefinitionRegistry registro = new SchemaParser().parse(SDL);

        RuntimeWiring wiring = newRuntimeWiring()
                .type("Query", builder -> builder
                        .dataFetcher("roles", env -> repositorio.listar())
                        .dataFetcher("rol", env ->
                                repositorio.buscarPorId(Long.parseLong(env.getArgument("id"))).orElse(null)))
                .type("Mutation", builder -> builder
                        .dataFetcher("crearRol", env -> {
                            Rol rol = new Rol();
                            rol.setNombre(env.getArgument("nombre"));
                            rol.setDescripcion(env.getArgument("descripcion"));
                            Rol creado = repositorio.crear(rol);
                            PublicadorEventos.rolCreado(creado);
                            return creado;
                        })
                        .dataFetcher("actualizarRol", env -> {
                            long id = Long.parseLong(env.getArgument("id"));
                            Rol rol = new Rol();
                            rol.setNombre(env.getArgument("nombre"));
                            rol.setDescripcion(env.getArgument("descripcion"));
                            if (!repositorio.actualizar(id, rol)) {
                                return null;
                            }
                            rol.setId(id);
                            PublicadorEventos.rolActualizado(rol);
                            return rol;
                        })
                        .dataFetcher("eliminarRol", env -> {
                            long id = Long.parseLong(env.getArgument("id"));
                            Optional<Rol> previo = repositorio.buscarPorId(id);
                            if (previo.isEmpty() || !repositorio.eliminar(id)) {
                                return false;
                            }
                            PublicadorEventos.rolEliminado(previo.get());
                            return true;
                        }))
                .build();

        GraphQLSchema esquema = new SchemaGenerator().makeExecutableSchema(registro, wiring);
        return GraphQL.newGraphQL(esquema).build();
    }
}
