-- Callback do Flyway (roda antes de toda migrate; não é migration e não tem versão).
--
-- Prepara o banco para a V5__seed_prices num ambiente que já tinha os SKUs de
-- demonstração aprendidos por evento (produtos do antigo scripts/seed-demo.sh, criados
-- pela API antes da V7 do catálogo existir). Sem esta limpeza a V5 bate em
-- uk_known_sku_tenant_sku / uk_price_current e o serviço não sobe.
--
-- Só age ENQUANTO a V5 não foi aplicada; depois disso é no-op para sempre. Banco novo:
-- nada a apagar. O catalog-service tem o callback equivalente para a V7__seed_products.
-- Gerado a partir da V5: se ela mudar SKUs, atualizar a lista abaixo.

DO $$
DECLARE
    v_tenant uuid := 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
    v_skus text[] := ARRAY[
        'BASEUS-USBC-2M-PT',
        'PULSE-CAPA-R18P-VD',
        'XIAOMI-BUDS6PLAY-BR',
        'ANKER-SPACEONE-PT',
        'REDMI18-128-BLK',
        'XIAOMI-WATCH5A-PT',
        'SAMSUNG-GALAXY-A55-5G',
        'IWILL-TELA-DE',
        'DELL-NOTEBOOK',
        'LENOVO-NOTEBOOK-IDEAPAD',
        'KINGSTON-SSD-NVME-1TB-NV2',
        'SAMSUNG-TAB-A9-128GB-WI',
        'LENOVO-TAB-M11-TB330',
        'LOGITECH-MOUSE-SEM-FIO-MX',
        'EPSON-IMPRESSORA',
        'CANON-CAMERA',
        'GOPRO-CAMERA-DE-ACAO',
        'VIVITAR-TRIPE-DE',
        'SONY-FILMADORA',
        'JBL-CAIXA-DE-SOM',
        'AMAZFIT-AMAZFIT-BIP-5',
        'AOC-SOUNDBAR-2-1',
        'SONY-CONSOLE',
        'MICROSOFT-CONSOLE-XBOX',
        'SONY-CONTROLE-SEM-FIO',
        'TCL-SMART-TV-55-4K',
        'PHILCO-SMART-TV-43-FULL',
        'BRASFORMA-SUPORTE',
        'BRASTEMP-GELADEIRA-FROST',
        'ELECTROLUX-GELADEIRA-FROST',
        'ATLAS-FOGAO-5-BOCAS',
        'ELECTROLUX-LAVADORA-DE',
        'BRASTEMP-LAVA-E-SECA-11KG',
        'MADESA-ESTANTE',
        'RUDNICK-RACK-PARA-TV-ATE',
        'TASCHIBRA-LAMPADA-LED',
        'TASCHIBRA-LUMINARIA',
        'TRAMONTINA-CONJUNTO-DE',
        'MONDIAL-VENTILADOR-DE',
        'BRITANIA-CLIMATIZADOR-DE',
        'MIDEA-AR-CONDICIONADO',
        'LG-AR-CONDICIONADO',
        'HERING-CAMISETA-BASICA',
        'MALWEE-CAMISETA',
        'HERING-CALCA-JEANS-SLIM',
        'MALWEE-CALCA-JOGGER',
        'NIKE-TENIS-DE-CORRIDA',
        'OLYMPIKUS-TENIS-CASUAL',
        'ZINCO-VESTIDO-MIDI-DE',
        'FARM-RIO-VESTIDO-FLORAL',
        'HERING-BLUSA-DE-TRICO',
        'COLCCI-BLUSA-CROPPED',
        'VIZZANO-SANDALIA',
        'AREZZO-TENIS-CASUAL',
        'SCHUTZ-BOLSA-TIRACOLO',
        'CAPODARTE-MOCHILA-CASUAL',
        'RAY-BAN-OCULOS-DE-SOL',
        'CHILLI-BEANS-OCULOS-DE-SOL',
        'TECHNOS-RELOGIO-DIGITAL',
        'MORMAII-RELOGIO',
        'OXER-KIT-DE-HALTERES',
        'GROWTH-WHEY-PROTEIN',
        'MAX-TITANIUM-CREATINA',
        'CALOI-BICICLETA-ARO-29',
        'OGGI-BICICLETA-SPEED',
        'HIGH-ONE-CAPACETE',
        'NAUTIKA-BARRACA-DE',
        'GUEPARDO-SACO-DE-DORMIR',
        'NAUTIKA-MOCHILA-DE',
        'GUEPARDO-BASTAO-RETRATIL',
        'O-BOTICARIO-EAU-DE-PARFUM',
        'NATURA-EAU-DE-TOILETTE',
        'O-BOTICARIO-EAU-DE-PARFUM-2',
        'NATURA-DEO-COLONIA',
        'NIVEA-HIDRATANTE',
        'NATURA-CREME-FACIAL',
        'LA-ROCHE-POSAY-PROTETOR-SOLAR',
        'SUNDOWN-PROTETOR-SOLAR',
        'PANTENE-SHAMPOO',
        'SEDA-CONDICIONADOR',
        'TAIFF-SECADOR-DE',
        'ESTRELA-BONECA-COM',
        'MATTEL-BONECO-DE-ACAO',
        'GROW-QUEBRA-CABECA',
        'GROW-JOGO-DE',
        'PAMPERS-FRALDAS',
        'HUGGIES-LENCOS',
        'BURIGOTTO-CARRINHO-DE-BEBE',
        'GALZERANO-CADEIRA-PARA',
        'COMPANHIA-DAS-TORTO-ARADO',
        'COMPANHIA-DAS-GRANDE-SERTAO',
        'EDITORA-O-APANHADOR-NO',
        'FABER-CASTELL-ESTOJO-DE',
        'TILIBRA-CADERNO',
        'BIC-CANETA',
        'XFX-PLACA-DE-VIDEO',
        'MULTILASER-HUB-USB-C-7-EM-1',
        'BRASTEMP-COOKTOP-5-BOCAS',
        'TRAMONTINA-JOGO-DE-FACAS-EM',
        'EDITORA-1984'
    ];
    v_removed int;
BEGIN
    -- Tudo no schema que o Flyway está migrando (current_schema): nome solto resolveria
    -- pelo search_path e podia cair em OUTRO schema que já tem as tabelas.
    IF to_regclass(format('%I.flyway_schema_history', current_schema())) IS NULL
       OR to_regclass(format('%I.tb_known_sku', current_schema())) IS NULL THEN
        RETURN;
    END IF;
    IF EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '5' AND success) THEN
        RETURN;
    END IF;

    DELETE FROM tb_known_sku WHERE tenant_id = v_tenant AND sku = ANY (v_skus);
    GET DIAGNOSTICS v_removed = ROW_COUNT;
    -- histórico de preço desses SKUs era só de demonstração; a V5 grava o vigente de novo
    DELETE FROM tb_price     WHERE tenant_id = v_tenant AND sku = ANY (v_skus);

    IF v_removed > 0 THEN
        RAISE NOTICE 'seed: % SKU(s) de demonstração antigos removidos antes da V5', v_removed;
    END IF;
END $$;
