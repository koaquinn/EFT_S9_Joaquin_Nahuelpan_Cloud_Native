ALTER SESSION SET CONTAINER = XEPDB1;
ALTER SESSION SET CURRENT_SCHEMA = APPUSER;

INSERT INTO APPUSER.ROLES (NOMBRE, DESCRIPCION) VALUES ('ADMIN', 'Administrador del sistema con acceso total');
INSERT INTO APPUSER.ROLES (NOMBRE, DESCRIPCION) VALUES ('OPERADOR', 'Gestiona la operación diaria del sistema');
INSERT INTO APPUSER.ROLES (NOMBRE, DESCRIPCION) VALUES ('CONSULTA', 'Acceso de solo lectura a la información');

INSERT INTO APPUSER.USUARIOS (NOMBRE, USERNAME, EMAIL, ACTIVO, ROL_ID)
VALUES ('Joaquin Nahuelpan', 'jnahuelpan', 'joaquin.nahuelpan@duocuc.cl', 1, 1);

INSERT INTO APPUSER.USUARIOS (NOMBRE, USERNAME, EMAIL, ACTIVO, ROL_ID)
VALUES ('Maria Gonzalez', 'mgonzalez', 'maria.gonzalez@duocuc.cl', 1, 2);

INSERT INTO APPUSER.USUARIOS (NOMBRE, USERNAME, EMAIL, ACTIVO, ROL_ID)
VALUES ('Pedro Soto', 'psoto', 'pedro.soto@duocuc.cl', 0, 3);

COMMIT;
