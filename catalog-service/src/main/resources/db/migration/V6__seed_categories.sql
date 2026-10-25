-- V6: popula a árvore de categorias (3 níveis) do tenant de dev usado pelo
-- frontend (packages/catalog/src/config/api.ts, apps/catalog-mfe/src/lib/http.ts),
-- para o mega-menu de categorias ter dados reais, estilo Mercado Livre.
-- Ids de raiz/grupo em variáveis (gen_random_uuid(), nativo desde o PG13) porque
-- servem de parent_id para os níveis abaixo; folhas não precisam de variável.

DO $$
DECLARE
    v_tenant uuid := 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';

    r1   uuid := gen_random_uuid(); -- Tecnologia
    r1g1 uuid := gen_random_uuid(); -- Celulares e Telefones
    r1g2 uuid := gen_random_uuid(); -- Informática
    r1g3 uuid := gen_random_uuid(); -- Câmeras e Acessórios
    r1g4 uuid := gen_random_uuid(); -- Eletrônicos, Áudio e Vídeo
    r1g5 uuid := gen_random_uuid(); -- Games
    r1g6 uuid := gen_random_uuid(); -- Televisores

    r2   uuid := gen_random_uuid(); -- Casa e Eletrodomésticos
    r2g1 uuid := gen_random_uuid(); -- Eletrodomésticos
    r2g2 uuid := gen_random_uuid(); -- Casa e Decoração
    r2g3 uuid := gen_random_uuid(); -- Climatização

    r3   uuid := gen_random_uuid(); -- Moda e Vestuário
    r3g1 uuid := gen_random_uuid(); -- Moda Masculina
    r3g2 uuid := gen_random_uuid(); -- Moda Feminina
    r3g3 uuid := gen_random_uuid(); -- Acessórios de Moda

    r4   uuid := gen_random_uuid(); -- Esportes e Fitness
    r4g1 uuid := gen_random_uuid(); -- Fitness e Musculação
    r4g2 uuid := gen_random_uuid(); -- Ciclismo
    r4g3 uuid := gen_random_uuid(); -- Esportes de Aventura

    r5   uuid := gen_random_uuid(); -- Beleza e Cuidado Pessoal
    r5g1 uuid := gen_random_uuid(); -- Perfumaria
    r5g2 uuid := gen_random_uuid(); -- Cuidados com a Pele
    r5g3 uuid := gen_random_uuid(); -- Cuidados com o Cabelo

    r6   uuid := gen_random_uuid(); -- Brinquedos e Bebês
    r6g1 uuid := gen_random_uuid(); -- Brinquedos
    r6g2 uuid := gen_random_uuid(); -- Bebês

    r7   uuid := gen_random_uuid(); -- Livros e Papelaria
    r7g1 uuid := gen_random_uuid(); -- Livros
    r7g2 uuid := gen_random_uuid(); -- Papelaria
BEGIN

-- ===================== Tecnologia =====================
INSERT INTO tb_category (id, created_at, updated_at, version, tenant_id, depth, path, position, slug, status, title, parent_id) VALUES
(r1,   now(), now(), 0, v_tenant, 0, '/tecnologia', 0, 'tecnologia', 'ACTIVE', 'Tecnologia', NULL),
(r1g1, now(), now(), 0, v_tenant, 1, '/tecnologia/celulares-e-telefones', 0, 'celulares-e-telefones', 'ACTIVE', 'Celulares e Telefones', r1),
(r1g2, now(), now(), 0, v_tenant, 1, '/tecnologia/informatica', 1, 'informatica', 'ACTIVE', 'Informática', r1),
(r1g3, now(), now(), 0, v_tenant, 1, '/tecnologia/cameras-e-acessorios', 2, 'cameras-e-acessorios', 'ACTIVE', 'Câmeras e Acessórios', r1),
(r1g4, now(), now(), 0, v_tenant, 1, '/tecnologia/eletronicos-audio-e-video', 3, 'eletronicos-audio-e-video', 'ACTIVE', 'Eletrônicos, Áudio e Vídeo', r1),
(r1g5, now(), now(), 0, v_tenant, 1, '/tecnologia/games', 4, 'games', 'ACTIVE', 'Games', r1),
(r1g6, now(), now(), 0, v_tenant, 1, '/tecnologia/televisores', 5, 'televisores', 'ACTIVE', 'Televisores', r1);

INSERT INTO tb_category (id, created_at, updated_at, version, tenant_id, depth, path, position, slug, status, title, parent_id) VALUES
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/tecnologia/celulares-e-telefones/smartphones', 0, 'smartphones', 'ACTIVE', 'Smartphones', r1g1),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/tecnologia/celulares-e-telefones/acessorios-para-celulares', 1, 'acessorios-para-celulares', 'ACTIVE', 'Acessórios para Celulares', r1g1),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/tecnologia/celulares-e-telefones/pecas-para-celular', 2, 'pecas-para-celular', 'ACTIVE', 'Peças para Celular', r1g1),

(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/tecnologia/informatica/notebooks', 0, 'notebooks', 'ACTIVE', 'Notebooks', r1g2),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/tecnologia/informatica/componentes-para-pc', 1, 'componentes-para-pc', 'ACTIVE', 'Componentes para PC', r1g2),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/tecnologia/informatica/tablets-e-acessorios', 2, 'tablets-e-acessorios', 'ACTIVE', 'Tablets e Acessórios', r1g2),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/tecnologia/informatica/acessorios-para-notebook', 3, 'acessorios-para-notebook', 'ACTIVE', 'Acessórios para Notebook', r1g2),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/tecnologia/informatica/impressoras', 4, 'impressoras', 'ACTIVE', 'Impressoras', r1g2),

(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/tecnologia/cameras-e-acessorios/cameras', 0, 'cameras', 'ACTIVE', 'Câmeras', r1g3),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/tecnologia/cameras-e-acessorios/acessorios-para-cameras', 1, 'acessorios-para-cameras', 'ACTIVE', 'Acessórios para Câmeras', r1g3),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/tecnologia/cameras-e-acessorios/filmadoras', 2, 'filmadoras', 'ACTIVE', 'Filmadoras', r1g3),

(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/tecnologia/eletronicos-audio-e-video/fones-de-ouvido', 0, 'fones-de-ouvido', 'ACTIVE', 'Fones de Ouvido', r1g4),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/tecnologia/eletronicos-audio-e-video/caixas-de-som-e-audio-portatil', 1, 'caixas-de-som-e-audio-portatil', 'ACTIVE', 'Caixas de Som e Áudio Portátil', r1g4),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/tecnologia/eletronicos-audio-e-video/smartwatches', 2, 'smartwatches', 'ACTIVE', 'Smartwatches', r1g4),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/tecnologia/eletronicos-audio-e-video/acessorios-para-tv', 3, 'acessorios-para-tv', 'ACTIVE', 'Acessórios para TV', r1g4),

(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/tecnologia/games/video-games', 0, 'video-games', 'ACTIVE', 'Video Games', r1g5),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/tecnologia/games/acessorios-para-games', 1, 'acessorios-para-games', 'ACTIVE', 'Acessórios para Games', r1g5),

(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/tecnologia/televisores/smart-tvs', 0, 'smart-tvs', 'ACTIVE', 'Smart TVs', r1g6),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/tecnologia/televisores/suportes-e-acessorios-para-tv', 1, 'suportes-e-acessorios-para-tv', 'ACTIVE', 'Suportes e Acessórios para TV', r1g6);

-- ===================== Casa e Eletrodomésticos =====================
INSERT INTO tb_category (id, created_at, updated_at, version, tenant_id, depth, path, position, slug, status, title, parent_id) VALUES
(r2,   now(), now(), 0, v_tenant, 0, '/casa-e-eletrodomesticos', 1, 'casa-e-eletrodomesticos', 'ACTIVE', 'Casa e Eletrodomésticos', NULL),
(r2g1, now(), now(), 0, v_tenant, 1, '/casa-e-eletrodomesticos/eletrodomesticos', 0, 'eletrodomesticos', 'ACTIVE', 'Eletrodomésticos', r2),
(r2g2, now(), now(), 0, v_tenant, 1, '/casa-e-eletrodomesticos/casa-e-decoracao', 1, 'casa-e-decoracao', 'ACTIVE', 'Casa e Decoração', r2),
(r2g3, now(), now(), 0, v_tenant, 1, '/casa-e-eletrodomesticos/climatizacao', 2, 'climatizacao', 'ACTIVE', 'Climatização', r2);

INSERT INTO tb_category (id, created_at, updated_at, version, tenant_id, depth, path, position, slug, status, title, parent_id) VALUES
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/casa-e-eletrodomesticos/eletrodomesticos/geladeiras', 0, 'geladeiras', 'ACTIVE', 'Geladeiras', r2g1),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/casa-e-eletrodomesticos/eletrodomesticos/fogoes-e-cooktops', 1, 'fogoes-e-cooktops', 'ACTIVE', 'Fogões e Cooktops', r2g1),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/casa-e-eletrodomesticos/eletrodomesticos/lavanderia', 2, 'lavanderia', 'ACTIVE', 'Lavanderia', r2g1),

(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/casa-e-eletrodomesticos/casa-e-decoracao/moveis', 0, 'moveis', 'ACTIVE', 'Móveis', r2g2),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/casa-e-eletrodomesticos/casa-e-decoracao/iluminacao', 1, 'iluminacao', 'ACTIVE', 'Iluminação', r2g2),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/casa-e-eletrodomesticos/casa-e-decoracao/utilidades-domesticas', 2, 'utilidades-domesticas', 'ACTIVE', 'Utilidades Domésticas', r2g2),

(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/casa-e-eletrodomesticos/climatizacao/ventiladores', 0, 'ventiladores', 'ACTIVE', 'Ventiladores', r2g3),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/casa-e-eletrodomesticos/climatizacao/ar-condicionado', 1, 'ar-condicionado', 'ACTIVE', 'Ar-Condicionado', r2g3);

-- ===================== Moda e Vestuário =====================
INSERT INTO tb_category (id, created_at, updated_at, version, tenant_id, depth, path, position, slug, status, title, parent_id) VALUES
(r3,   now(), now(), 0, v_tenant, 0, '/moda-e-vestuario', 2, 'moda-e-vestuario', 'ACTIVE', 'Moda e Vestuário', NULL),
(r3g1, now(), now(), 0, v_tenant, 1, '/moda-e-vestuario/moda-masculina', 0, 'moda-masculina', 'ACTIVE', 'Moda Masculina', r3),
(r3g2, now(), now(), 0, v_tenant, 1, '/moda-e-vestuario/moda-feminina', 1, 'moda-feminina', 'ACTIVE', 'Moda Feminina', r3),
(r3g3, now(), now(), 0, v_tenant, 1, '/moda-e-vestuario/acessorios-de-moda', 2, 'acessorios-de-moda', 'ACTIVE', 'Acessórios de Moda', r3);

INSERT INTO tb_category (id, created_at, updated_at, version, tenant_id, depth, path, position, slug, status, title, parent_id) VALUES
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/moda-e-vestuario/moda-masculina/camisetas', 0, 'camisetas', 'ACTIVE', 'Camisetas', r3g1),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/moda-e-vestuario/moda-masculina/calcas', 1, 'calcas', 'ACTIVE', 'Calças', r3g1),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/moda-e-vestuario/moda-masculina/calcados-masculinos', 2, 'calcados-masculinos', 'ACTIVE', 'Calçados Masculinos', r3g1),

(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/moda-e-vestuario/moda-feminina/vestidos', 0, 'vestidos', 'ACTIVE', 'Vestidos', r3g2),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/moda-e-vestuario/moda-feminina/blusas', 1, 'blusas', 'ACTIVE', 'Blusas', r3g2),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/moda-e-vestuario/moda-feminina/calcados-femininos', 2, 'calcados-femininos', 'ACTIVE', 'Calçados Femininos', r3g2),

(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/moda-e-vestuario/acessorios-de-moda/bolsas', 0, 'bolsas', 'ACTIVE', 'Bolsas', r3g3),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/moda-e-vestuario/acessorios-de-moda/oculos-de-sol', 1, 'oculos-de-sol', 'ACTIVE', 'Óculos de Sol', r3g3),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/moda-e-vestuario/acessorios-de-moda/relogios', 2, 'relogios', 'ACTIVE', 'Relógios', r3g3);

-- ===================== Esportes e Fitness =====================
INSERT INTO tb_category (id, created_at, updated_at, version, tenant_id, depth, path, position, slug, status, title, parent_id) VALUES
(r4,   now(), now(), 0, v_tenant, 0, '/esportes-e-fitness', 3, 'esportes-e-fitness', 'ACTIVE', 'Esportes e Fitness', NULL),
(r4g1, now(), now(), 0, v_tenant, 1, '/esportes-e-fitness/fitness-e-musculacao', 0, 'fitness-e-musculacao', 'ACTIVE', 'Fitness e Musculação', r4),
(r4g2, now(), now(), 0, v_tenant, 1, '/esportes-e-fitness/ciclismo', 1, 'ciclismo', 'ACTIVE', 'Ciclismo', r4),
(r4g3, now(), now(), 0, v_tenant, 1, '/esportes-e-fitness/esportes-de-aventura', 2, 'esportes-de-aventura', 'ACTIVE', 'Esportes de Aventura', r4);

INSERT INTO tb_category (id, created_at, updated_at, version, tenant_id, depth, path, position, slug, status, title, parent_id) VALUES
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/esportes-e-fitness/fitness-e-musculacao/equipamentos-de-ginastica', 0, 'equipamentos-de-ginastica', 'ACTIVE', 'Equipamentos de Ginástica', r4g1),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/esportes-e-fitness/fitness-e-musculacao/suplementos', 1, 'suplementos', 'ACTIVE', 'Suplementos', r4g1),

(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/esportes-e-fitness/ciclismo/bicicletas', 0, 'bicicletas', 'ACTIVE', 'Bicicletas', r4g2),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/esportes-e-fitness/ciclismo/acessorios-para-bicicleta', 1, 'acessorios-para-bicicleta', 'ACTIVE', 'Acessórios para Bicicleta', r4g2),

(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/esportes-e-fitness/esportes-de-aventura/camping', 0, 'camping', 'ACTIVE', 'Camping', r4g3),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/esportes-e-fitness/esportes-de-aventura/trilha', 1, 'trilha', 'ACTIVE', 'Trilha', r4g3);

-- ===================== Beleza e Cuidado Pessoal =====================
INSERT INTO tb_category (id, created_at, updated_at, version, tenant_id, depth, path, position, slug, status, title, parent_id) VALUES
(r5,   now(), now(), 0, v_tenant, 0, '/beleza-e-cuidado-pessoal', 4, 'beleza-e-cuidado-pessoal', 'ACTIVE', 'Beleza e Cuidado Pessoal', NULL),
(r5g1, now(), now(), 0, v_tenant, 1, '/beleza-e-cuidado-pessoal/perfumaria', 0, 'perfumaria', 'ACTIVE', 'Perfumaria', r5),
(r5g2, now(), now(), 0, v_tenant, 1, '/beleza-e-cuidado-pessoal/cuidados-com-a-pele', 1, 'cuidados-com-a-pele', 'ACTIVE', 'Cuidados com a Pele', r5),
(r5g3, now(), now(), 0, v_tenant, 1, '/beleza-e-cuidado-pessoal/cuidados-com-o-cabelo', 2, 'cuidados-com-o-cabelo', 'ACTIVE', 'Cuidados com o Cabelo', r5);

INSERT INTO tb_category (id, created_at, updated_at, version, tenant_id, depth, path, position, slug, status, title, parent_id) VALUES
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/beleza-e-cuidado-pessoal/perfumaria/perfumes-femininos', 0, 'perfumes-femininos', 'ACTIVE', 'Perfumes Femininos', r5g1),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/beleza-e-cuidado-pessoal/perfumaria/perfumes-masculinos', 1, 'perfumes-masculinos', 'ACTIVE', 'Perfumes Masculinos', r5g1),

(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/beleza-e-cuidado-pessoal/cuidados-com-a-pele/hidratantes', 0, 'hidratantes', 'ACTIVE', 'Hidratantes', r5g2),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/beleza-e-cuidado-pessoal/cuidados-com-a-pele/protetores-solares', 1, 'protetores-solares', 'ACTIVE', 'Protetores Solares', r5g2),

(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/beleza-e-cuidado-pessoal/cuidados-com-o-cabelo/shampoos-e-condicionadores', 0, 'shampoos-e-condicionadores', 'ACTIVE', 'Shampoos e Condicionadores', r5g3),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/beleza-e-cuidado-pessoal/cuidados-com-o-cabelo/secadores-e-pranchas', 1, 'secadores-e-pranchas', 'ACTIVE', 'Secadores e Pranchas', r5g3);

-- ===================== Brinquedos e Bebês =====================
INSERT INTO tb_category (id, created_at, updated_at, version, tenant_id, depth, path, position, slug, status, title, parent_id) VALUES
(r6,   now(), now(), 0, v_tenant, 0, '/brinquedos-e-bebes', 5, 'brinquedos-e-bebes', 'ACTIVE', 'Brinquedos e Bebês', NULL),
(r6g1, now(), now(), 0, v_tenant, 1, '/brinquedos-e-bebes/brinquedos', 0, 'brinquedos', 'ACTIVE', 'Brinquedos', r6),
(r6g2, now(), now(), 0, v_tenant, 1, '/brinquedos-e-bebes/bebes', 1, 'bebes', 'ACTIVE', 'Bebês', r6);

INSERT INTO tb_category (id, created_at, updated_at, version, tenant_id, depth, path, position, slug, status, title, parent_id) VALUES
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/brinquedos-e-bebes/brinquedos/bonecas-e-bonecos', 0, 'bonecas-e-bonecos', 'ACTIVE', 'Bonecas e Bonecos', r6g1),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/brinquedos-e-bebes/brinquedos/jogos-e-quebra-cabecas', 1, 'jogos-e-quebra-cabecas', 'ACTIVE', 'Jogos e Quebra-cabeças', r6g1),

(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/brinquedos-e-bebes/bebes/fraldas-e-higiene', 0, 'fraldas-e-higiene', 'ACTIVE', 'Fraldas e Higiene', r6g2),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/brinquedos-e-bebes/bebes/carrinhos-e-acessorios', 1, 'carrinhos-e-acessorios', 'ACTIVE', 'Carrinhos e Acessórios', r6g2);

-- ===================== Livros e Papelaria =====================
INSERT INTO tb_category (id, created_at, updated_at, version, tenant_id, depth, path, position, slug, status, title, parent_id) VALUES
(r7,   now(), now(), 0, v_tenant, 0, '/livros-e-papelaria', 6, 'livros-e-papelaria', 'ACTIVE', 'Livros e Papelaria', NULL),
(r7g1, now(), now(), 0, v_tenant, 1, '/livros-e-papelaria/livros', 0, 'livros', 'ACTIVE', 'Livros', r7),
(r7g2, now(), now(), 0, v_tenant, 1, '/livros-e-papelaria/papelaria', 1, 'papelaria', 'ACTIVE', 'Papelaria', r7);

INSERT INTO tb_category (id, created_at, updated_at, version, tenant_id, depth, path, position, slug, status, title, parent_id) VALUES
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/livros-e-papelaria/livros/literatura-nacional', 0, 'literatura-nacional', 'ACTIVE', 'Literatura Nacional', r7g1),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/livros-e-papelaria/livros/literatura-estrangeira', 1, 'literatura-estrangeira', 'ACTIVE', 'Literatura Estrangeira', r7g1),

(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/livros-e-papelaria/papelaria/material-escolar', 0, 'material-escolar', 'ACTIVE', 'Material Escolar', r7g2),
(gen_random_uuid(), now(), now(), 0, v_tenant, 2, '/livros-e-papelaria/papelaria/material-de-escritorio', 1, 'material-de-escritorio', 'ACTIVE', 'Material de Escritório', r7g2);

END $$;
