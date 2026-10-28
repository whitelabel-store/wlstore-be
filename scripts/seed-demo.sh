#!/usr/bin/env bash
# =============================================================================
# seed-demo.sh — cadastra o catálogo de demonstração (seed/demo-catalog.json)
# num ambiente, PELA API (não por SQL): criar produto publica o evento que o
# pricing-service consome, igual a um cadastro feito no painel.
#
#   scripts/seed-demo.sh local      backend na sua máquina (localhost:8081/8082)
#   scripts/seed-demo.sh develop    cluster (precisa do /etc/hosts: make hosts na infra)
#   scripts/seed-demo.sh prod       idem; pede confirmação (YES=1 pula)
#
# Pode rodar de novo: categoria e produto que já existem (pelo título) são
# pulados; o preço é regravado (PUT). As categorias da migration V6 já existem
# em todo banco; o arquivo só traz as árvores extras que os produtos usam.
#
# Variáveis opcionais: CATALOG_URL, PRICING_URL, TENANT_ID, SEED_FILE.
# Requer: curl, jq.
# =============================================================================

set -euo pipefail

ENV_NAME="${1:-}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SEED_FILE="${SEED_FILE:-$ROOT/seed/demo-catalog.json}"
TENANT_ID="${TENANT_ID:-aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa}"

case "$ENV_NAME" in
  local)   CATALOG_URL="${CATALOG_URL:-http://localhost:8081}";  PRICING_URL="${PRICING_URL:-http://localhost:8082}" ;;
  develop) CATALOG_URL="${CATALOG_URL:-http://develop.catalog-service.wlstore.local}"; PRICING_URL="${PRICING_URL:-http://develop.pricing-service.wlstore.local}" ;;
  prod)    CATALOG_URL="${CATALOG_URL:-http://catalog-service.wlstore.local}"; PRICING_URL="${PRICING_URL:-http://pricing-service.wlstore.local}" ;;
  *) echo "Uso: $0 <local|develop|prod>"; exit 1 ;;
esac

for bin in curl jq; do command -v "$bin" >/dev/null || { echo "❌ precisa de $bin"; exit 1; }; done

if [[ "$ENV_NAME" == prod && "${YES:-}" != 1 ]]; then
  echo "⚠️  Isto cadastra produtos de DEMONSTRAÇÃO em produção ($CATALOG_URL)."
  read -r -p "Digite prod para confirmar: " ANS
  [[ "$ANS" == prod ]] || { echo "Cancelado."; exit 1; }
fi

H=(-H "X-Tenant-Id: $TENANT_ID" -H "Content-Type: application/json")

# api <método> <url> [json] → corpo em $RESP, código HTTP em $HTTP_STATUS.
# (Variáveis globais, não `$(api ...)`: um subshell não devolveria o status.)
api() {
  local out; out="$(mktemp)"
  HTTP_STATUS="$(curl -sS -o "$out" -w '%{http_code}' -X "$1" "${H[@]}" ${3:+--data "$3"} "$2")" || {
    rm -f "$out"; echo "❌ sem resposta de $2 (o serviço está no ar? o host resolve?)" >&2; exit 1; }
  RESP="$(cat "$out")"; rm -f "$out"
}

# Mapa "Raiz > Filho > Neto" → id, de todas as categorias do tenant.
category_paths() {
  local page=0 all="[]" body
  while :; do
    api GET "$CATALOG_URL/api/v1/categories?size=100&page=$page"; body="$RESP"
    [[ "$HTTP_STATUS" == 200 ]] || { echo "❌ GET categorias: $HTTP_STATUS $body" >&2; exit 1; }
    all="$(jq -c --argjson acc "$all" '$acc + .data' <<<"$body")"
    [[ "$(jq -r '.last' <<<"$body")" == true ]] && break
    page=$((page + 1))
  done
  jq -c 'def walk_paths($prefix): .[] | ($prefix + [.title]) as $p
           | {key: ($p | join(" > ")), value: .id}, (.children // [] | walk_paths($p));
         [walk_paths([])] | from_entries' <<<"$all"
}

echo "▸ Seed de demonstração → $ENV_NAME ($CATALOG_URL, tenant $TENANT_ID)"
PATHS="$(category_paths)"

# --- categorias extras (árvore inteira, se a raiz ainda não existe) -----------
while read -r cat; do
  root="$(jq -r '.title' <<<"$cat")"
  if jq -e --arg r "$root" 'has($r)' <<<"$PATHS" >/dev/null; then
    echo "  = categoria $root (já existe)"
  else
    api POST "$CATALOG_URL/api/v1/categories" "$cat"; body="$RESP"
    [[ "$HTTP_STATUS" == 201 ]] || { echo "❌ categoria $root: $HTTP_STATUS $body"; exit 1; }
    echo "  + categoria $root (com subcategorias)"
  fi
done < <(jq -c '.categories[]?' "$SEED_FILE")
PATHS="$(category_paths)"

# --- produtos -------------------------------------------------------------------
CREATED=0; SKIPPED=0
while read -r prod; do
  title="$(jq -r '.title' <<<"$prod")"
  cpath="$(jq -r '.category' <<<"$prod")"
  cid="$(jq -r --arg p "$cpath" '.[$p] // empty' <<<"$PATHS")"
  [[ -n "$cid" ]] || { echo "❌ categoria não encontrada: \"$cpath\" (produto \"$title\")"; exit 1; }

  api GET "$CATALOG_URL/api/v1/products?size=100&title=$(jq -rn --arg t "$title" '$t|@uri')"; existing="$RESP"
  if jq -e --arg t "$title" '.data | any(.title == $t)' <<<"$existing" >/dev/null; then
    echo "  = $title (já existe)"; SKIPPED=$((SKIPPED + 1))
  else
    req="$(jq -c --arg cid "$cid" '{title, brand, description, status, category: {id: $cid},
            variants: [.variants[] | del(.price)], images: (.images // [])}' <<<"$prod")"
    api POST "$CATALOG_URL/api/v1/products" "$req"; body="$RESP"
    [[ "$HTTP_STATUS" == 201 ]] || { echo "❌ produto \"$title\": $HTTP_STATUS $body"; exit 1; }
    echo "  + $title"; CREATED=$((CREATED + 1))
  fi

  # Preço: o pricing só aceita SKU que já recebeu pelo evento do catálogo;
  # logo depois de criar, tenta de novo por alguns segundos.
  while read -r v; do
    sku="$(jq -r '.sku' <<<"$v")"; price="$(jq -c '.price' <<<"$v")"
    for _ in $(seq 1 20); do
      api PUT "$PRICING_URL/api/v1/prices/$sku" "$price"; body="$RESP"
      [[ "$HTTP_STATUS" =~ ^2 ]] && break
      sleep 1
    done
    [[ "$HTTP_STATUS" =~ ^2 ]] || { echo "❌ preço $sku: $HTTP_STATUS $body"; exit 1; }
    echo "      preço $sku = $(jq -r '.amountCents / 100' <<<"$price")"
  done < <(jq -c '.variants[] | select(.price != null)' <<<"$prod")
done < <(jq -c '.products[]' "$SEED_FILE")

echo "✅ $CREATED produto(s) criado(s), $SKIPPED já existia(m) — $ENV_NAME"
