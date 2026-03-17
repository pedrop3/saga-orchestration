INSERT INTO public.product (id, created_at, updated_at, code)
VALUES (1, NOW(), NOW(), 'COMIC_BOOKS') ON CONFLICT (id) DO NOTHING;

INSERT INTO public.product (id, created_at, updated_at, code)
VALUES (2, NOW(), NOW(), 'BOOKS') ON CONFLICT (id) DO NOTHING;

INSERT INTO public.product (id, created_at, updated_at, code)
VALUES (3, NOW(), NOW(), 'MOVIES') ON CONFLICT (id) DO NOTHING;

INSERT INTO public.product (id, created_at, updated_at, code)
VALUES (4, NOW(), NOW(), 'MUSIC') ON CONFLICT (id) DO NOTHING;