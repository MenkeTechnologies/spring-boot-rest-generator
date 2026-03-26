--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: activity; Type: TABLE; Schema: public
--

CREATE TABLE public.activity (
    activity_id integer NOT NULL,
    module_id integer NOT NULL,
    activity_table character varying(150) NOT NULL,
    name character varying(150) NOT NULL,
    description text NOT NULL,
    active character varying(1) NOT NULL DEFAULT 'Y',
    create_date timestamp without time zone NOT NULL,
    update_date timestamp without time zone DEFAULT now()
);

--
-- Name: activity_activity_id_seq; Type: SEQUENCE; Schema: public
--

CREATE SEQUENCE public.activity_activity_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: language; Type: TABLE; Schema: public
--

CREATE TABLE public.language (
    language_id integer NOT NULL,
    live character varying(1) NOT NULL DEFAULT 'N',
    name character varying(150) NOT NULL,
    description character varying(4000) NOT NULL,
    origin character varying(1000) NOT NULL,
    region character varying(1000) NOT NULL,
    num_of_speakers character varying(1000) NOT NULL,
    thumbnail_image_url character varying(500),
    header_img_url character varying(500),
    create_date timestamp without time zone NOT NULL,
    update_date timestamp without time zone DEFAULT now()
);

--
-- Name: language_level; Type: TABLE; Schema: public
--

CREATE TABLE public.language_level (
    language_level_id integer NOT NULL,
    language_id integer NOT NULL,
    name character varying(150) NOT NULL,
    description character varying(4000) NOT NULL,
    title character varying(150),
    go_text character varying(50),
    price integer NOT NULL,
    start_page_text character varying(4000) NOT NULL,
    finish_page_text character varying(4000) NOT NULL,
    active character varying(1) NOT NULL DEFAULT 'Y',
    create_date timestamp without time zone NOT NULL,
    update_date timestamp without time zone DEFAULT now(),
    encouragement character varying(200)
);

--
-- Name: module; Type: TABLE; Schema: public
--

CREATE TABLE public.module (
    module_id integer NOT NULL,
    language_level_id integer NOT NULL,
    video_content_id integer NOT NULL,
    name character varying(500),
    description character varying(4000) NOT NULL,
    module_flow_index integer NOT NULL,
    active character varying(1) NOT NULL DEFAULT 'Y',
    create_date timestamp without time zone NOT NULL,
    update_date timestamp without time zone DEFAULT now()
);

--
-- Name: "user"; Type: TABLE; Schema: public
--

CREATE TABLE public."user" (
    user_id integer NOT NULL,
    name character varying(150) NOT NULL,
    email character varying(250) NOT NULL,
    pw_hash character varying(400) NOT NULL,
    discount smallint DEFAULT 0,
    email_verified_flag character varying(1) NOT NULL DEFAULT 'N',
    create_date timestamp without time zone NOT NULL,
    update_date timestamp without time zone DEFAULT now(),
    verification_hash character varying(400),
    reset_hash character varying(400),
    free_trial_completed character varying(1) NOT NULL DEFAULT 'N'
);

--
-- Name: video_content; Type: TABLE; Schema: public
--

CREATE TABLE public.video_content (
    video_content_id integer NOT NULL,
    title character varying(500) NOT NULL,
    media_url character varying(4000) NOT NULL,
    value character varying(4000) NOT NULL,
    create_date timestamp without time zone NOT NULL,
    update_date timestamp without time zone DEFAULT now()
);

--
-- Name: payment; Type: TABLE; Schema: public
--

CREATE TABLE public.payment (
    payment_id integer NOT NULL,
    payment_method_info_id integer NOT NULL,
    subscription_id integer NOT NULL,
    amount integer NOT NULL,
    payment_token character varying(500),
    active character varying(1) NOT NULL DEFAULT 'Y',
    create_date timestamp without time zone NOT NULL,
    update_date timestamp without time zone DEFAULT now()
);

--
-- Name: settings; Type: TABLE; Schema: public
--

CREATE TABLE public.settings (
    settings_id integer NOT NULL,
    key character varying(50) NOT NULL,
    value character varying(4000) NOT NULL,
    create_date timestamp without time zone NOT NULL,
    update_date timestamp without time zone DEFAULT now()
);

--
-- Name: word_bank; Type: TABLE; Schema: public
--

CREATE TABLE public.word_bank (
    word_bank_id integer NOT NULL,
    module_id integer,
    target_word character varying(45),
    working_word character varying(45)
);

--
-- Name: type_test; Type: TABLE; Schema: public
--

CREATE TABLE public.type_test (
    id bigserial NOT NULL,
    name text NOT NULL,
    score double precision,
    rating real,
    amount numeric(10,2),
    is_active boolean DEFAULT true,
    birth_date date,
    login_time time without time zone
);

--
-- Primary keys
--

ALTER TABLE ONLY public.activity
    ADD CONSTRAINT activity_pkey PRIMARY KEY (activity_id);

ALTER TABLE ONLY public.language
    ADD CONSTRAINT language_pkey PRIMARY KEY (language_id);

ALTER TABLE ONLY public.language_level
    ADD CONSTRAINT language_level_pkey PRIMARY KEY (language_level_id);

ALTER TABLE ONLY public.module
    ADD CONSTRAINT module_pkey PRIMARY KEY (module_id);

ALTER TABLE ONLY public."user"
    ADD CONSTRAINT user_pkey PRIMARY KEY (user_id);

ALTER TABLE ONLY public.video_content
    ADD CONSTRAINT video_content_pkey PRIMARY KEY (video_content_id);

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT payment_pkey PRIMARY KEY (payment_id);

ALTER TABLE ONLY public.settings
    ADD CONSTRAINT settings_pkey PRIMARY KEY (settings_id);

ALTER TABLE ONLY public.word_bank
    ADD CONSTRAINT word_bank_pkey PRIMARY KEY (word_bank_id);

ALTER TABLE ONLY public.type_test
    ADD CONSTRAINT type_test_pkey PRIMARY KEY (id);

--
-- Foreign keys
--

ALTER TABLE ONLY public.activity
    ADD CONSTRAINT fk_activity_module FOREIGN KEY (module_id) REFERENCES public.module(module_id) ON DELETE CASCADE;

ALTER TABLE ONLY public.language_level
    ADD CONSTRAINT fk_language_level_language FOREIGN KEY (language_id) REFERENCES public.language(language_id);

ALTER TABLE ONLY public.module
    ADD CONSTRAINT fk_module_language_level FOREIGN KEY (language_level_id) REFERENCES public.language_level(language_level_id);

ALTER TABLE ONLY public.module
    ADD CONSTRAINT fk_module_video_content FOREIGN KEY (video_content_id) REFERENCES public.video_content(video_content_id);

ALTER TABLE ONLY public.word_bank
    ADD CONSTRAINT fk_word_bank_module FOREIGN KEY (module_id) REFERENCES public.module(module_id) ON DELETE CASCADE;

--
-- PostgreSQL database dump complete
--
