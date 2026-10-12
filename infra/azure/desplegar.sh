#!/usr/bin/env bash
set -euo pipefail
export MSYS_NO_PATHCONV=1

RAIZ="$(cd "$(dirname "$0")/../.." && (pwd -W 2>/dev/null || pwd))"
CONFIG="$RAIZ/infra/azure/.azure.env"

if [ ! -f "$CONFIG" ]; then
  SUFIJO=$(python -c "import secrets,string; print(''.join(secrets.choice(string.ascii_lowercase+string.digits) for _ in range(5)))")
  cat > "$CONFIG" <<EOF
SUFIJO=$SUFIJO
GRUPO=rg-dsy2207-eft
REGION=brazilsouth
ORACLE_ACI=aci-oracle-eft-$SUFIJO
ORACLE_DNS=oracle-eft-$SUFIJO
ORACLE_SYS_PASSWORD=Sys$(python -c "import secrets,string; print(''.join(secrets.choice(string.ascii_letters+string.digits) for _ in range(17)))")
ORACLE_APP_PASSWORD=App$(python -c "import secrets,string; print(''.join(secrets.choice(string.ascii_letters+string.digits) for _ in range(17)))")
STORAGE=stdsy2207eft$SUFIJO
FN_USUARIOS=fn-usuarios-eft-$SUFIJO
FN_ROLES=fn-roles-eft-$SUFIJO
FN_EVENTOS=fn-eventos-eft-$SUFIJO
TOPIC=egt-usuarios-roles-eft-$SUFIJO
PLAN_BFF=plan-bff-eft-$SUFIJO
APP_BFF=app-bff-eft-$SUFIJO
EOF
fi
source "$CONFIG"

echo "[1/10] Registrando proveedores"
az provider register --namespace Microsoft.EventGrid --wait
az provider register --namespace Microsoft.ContainerInstance --wait

echo "[2/10] Grupo de recursos $GRUPO ($REGION)"
az group create -n "$GRUPO" -l "$REGION" -o none

echo "[3/10] Oracle XE en Azure Container Instances"
az container create -g "$GRUPO" -n "$ORACLE_ACI" --image gvenzl/oracle-xe:21-slim --os-type Linux \
  --cpu 1 --memory 2.5 --ports 1521 --ip-address Public --dns-name-label "$ORACLE_DNS" \
  --environment-variables APP_USER=appuser \
  --secure-environment-variables ORACLE_PASSWORD="$ORACLE_SYS_PASSWORD" APP_USER_PASSWORD="$ORACLE_APP_PASSWORD" \
  --restart-policy OnFailure -o none
until az container logs -g "$GRUPO" -n "$ORACLE_ACI" 2>/dev/null | grep -q "DATABASE IS READY TO USE"; do sleep 15; done

FQDN="$ORACLE_DNS.$REGION.azurecontainer.io"
echo "[4/10] Esquema y datos iniciales en $FQDN"
for script in "$RAIZ/db/oracle/01_esquema.sql" "$RAIZ/db/oracle/02_datos_iniciales.sql"; do
  (cat "$script"; echo "EXIT;") | docker run -i --rm --entrypoint sqlplus gvenzl/oracle-xe:21-slim \
    -S "system/$ORACLE_SYS_PASSWORD@//$FQDN:1521/XEPDB1"
done

echo "[5/10] Storage y topic de Event Grid"
az storage account create -g "$GRUPO" -n "$STORAGE" -l "$REGION" --sku Standard_LRS --allow-blob-public-access false -o none
az eventgrid topic create -g "$GRUPO" -n "$TOPIC" -l "$REGION" --input-schema eventgridschema -o none

echo "[6/10] Function Apps (Java 17, plan de consumo)"
for app in "$FN_USUARIOS" "$FN_ROLES" "$FN_EVENTOS"; do
  az functionapp create -g "$GRUPO" -n "$app" --storage-account "$STORAGE" --consumption-plan-location "$REGION" \
    --runtime java --runtime-version 17 --functions-version 4 --os-type Linux --disable-app-insights true -o none
done

echo "[7/10] Configuracion de las Function Apps"
JDBC="jdbc:oracle:thin:@//$FQDN:1521/XEPDB1"
EG_ENDPOINT=$(az eventgrid topic show -g "$GRUPO" -n "$TOPIC" --query endpoint -o tsv)
EG_KEY=$(az eventgrid topic key list -g "$GRUPO" -n "$TOPIC" --query key1 -o tsv)
for app in "$FN_USUARIOS" "$FN_ROLES"; do
  az functionapp config appsettings set -g "$GRUPO" -n "$app" -o none --settings \
    "ORACLE_JDBC_URL=$JDBC" "ORACLE_USER=appuser" "ORACLE_PASSWORD=$ORACLE_APP_PASSWORD" \
    "EVENTGRID_TOPIC_ENDPOINT=$EG_ENDPOINT" "EVENTGRID_TOPIC_KEY=$EG_KEY"
done
az functionapp config appsettings set -g "$GRUPO" -n "$FN_EVENTOS" -o none --settings \
  "ORACLE_JDBC_URL=$JDBC" "ORACLE_USER=appuser" "ORACLE_PASSWORD=$ORACLE_APP_PASSWORD" \
  "ROL_POR_DEFECTO=CONSULTA" "ADMIN_EMAIL=administracion@duocuc.cl"

echo "[8/10] Compilacion y despliegue del codigo"
mkdir -p "$RAIZ/infra/azure/paquetes"
for par in "funcion-usuarios:$FN_USUARIOS" "funcion-roles:$FN_ROLES" "funcion-eventos:$FN_EVENTOS"; do
  modulo="${par%%:*}"
  app="${par##*:}"
  docker run --rm -v "$RAIZ/$modulo:/app" -v maven-cache:/root/.m2 -w /app maven:3.9-eclipse-temurin-17 mvn -B -q -DskipTests clean package
  (cd "$RAIZ/$modulo/target/azure-functions/$modulo" && rm -f local.settings.json \
    && python -c "import shutil; shutil.make_archive('$RAIZ/infra/azure/paquetes/$modulo', 'zip', '.')")
  az functionapp deployment source config-zip -g "$GRUPO" -n "$app" \
    --src "$RAIZ/infra/azure/paquetes/$modulo.zip" --timeout 600 -o none
done

echo "[9/10] Suscripciones de Event Grid"
until az functionapp function list -g "$GRUPO" -n "$FN_EVENTOS" --query "[].name" -o tsv | grep -q quitarRolUsuarios; do
  az rest --method post --uri "https://management.azure.com$(az functionapp show -g "$GRUPO" -n "$FN_EVENTOS" --query id -o tsv)/syncfunctiontriggers?api-version=2022-03-01" -o none || true
  sleep 15
done
TOPIC_ID=$(az eventgrid topic show -g "$GRUPO" -n "$TOPIC" --query id -o tsv)
FN_ID=$(az functionapp show -g "$GRUPO" -n "$FN_EVENTOS" --query id -o tsv)
P="Duoc.UsuariosRoles"
az eventgrid event-subscription create --name sub-auditoria --source-resource-id "$TOPIC_ID" \
  --endpoint-type azurefunction --endpoint "$FN_ID/functions/auditarEvento" --max-delivery-attempts 10 -o none
az eventgrid event-subscription create --name sub-notificaciones --source-resource-id "$TOPIC_ID" \
  --endpoint-type azurefunction --endpoint "$FN_ID/functions/notificarUsuario" \
  --included-event-types "$P.UsuarioCreado" "$P.UsuarioActualizado" "$P.UsuarioEliminado" --max-delivery-attempts 10 -o none
az eventgrid event-subscription create --name sub-rol-por-defecto --source-resource-id "$TOPIC_ID"   --endpoint-type azurefunction --endpoint "$FN_ID/functions/asignarRolPorDefecto"   --included-event-types "$P.UsuarioCreado" --max-delivery-attempts 10 -o none
az eventgrid event-subscription create --name sub-quitar-rol --source-resource-id "$TOPIC_ID"   --endpoint-type azurefunction --endpoint "$FN_ID/functions/quitarRolUsuarios"   --included-event-types "$P.RolEliminado" --max-delivery-attempts 10 -o none

echo "[10/10] BFF en Azure App Service (plan F1 gratuito)"
az appservice plan create -g "$GRUPO" -n "$PLAN_BFF" --is-linux --sku F1 -l "$REGION" -o none
az webapp create -g "$GRUPO" -p "$PLAN_BFF" -n "$APP_BFF" --runtime "JAVA:17-java17" -o none
az webapp config appsettings set -g "$GRUPO" -n "$APP_BFF" -o none --settings   "FUNCION_USUARIOS_URL=https://$FN_USUARIOS.azurewebsites.net"   "FUNCION_ROLES_URL=https://$FN_ROLES.azurewebsites.net"   "FUNCION_EVENTOS_URL=https://$FN_EVENTOS.azurewebsites.net"   "SERVER_PORT=80"
docker run --rm -v "$RAIZ/bff-usuarios-roles:/app" -v maven-cache:/root/.m2 -w /app maven:3.9-eclipse-temurin-17 mvn -B -q -DskipTests clean package
az webapp deploy -g "$GRUPO" -n "$APP_BFF" --src-path "$RAIZ/bff-usuarios-roles/target/bff-usuarios-roles-1.0.0.jar" --type jar -o none

echo
echo "Despliegue completo:"
echo "  BFF: https://$APP_BFF.azurewebsites.net/api/bff/usuarios"
echo "  https://$FN_USUARIOS.azurewebsites.net/api/usuarios"
echo "  https://$FN_ROLES.azurewebsites.net/api/roles"
echo "  https://$FN_EVENTOS.azurewebsites.net/api/auditoria"
echo "  https://$FN_EVENTOS.azurewebsites.net/api/notificaciones"
