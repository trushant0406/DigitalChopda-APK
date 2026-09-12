--
-- PostgreSQL database dump
--

\restrict pvGwKoBRxlXQ9JYzYfrmphPgSKD577nUSLdlp1Dd5aqXIxbn607sU1Oew9Wh76W

-- Dumped from database version 16.15
-- Dumped by pg_dump version 16.15

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: business_profile; Type: TABLE; Schema: public; Owner: callintel
--

CREATE TABLE public.business_profile (
    id smallint DEFAULT 1 NOT NULL,
    name text,
    address text,
    gstin text,
    pan text,
    mobile text,
    state text,
    bank_name text,
    bank_account_no text,
    bank_ifsc text,
    bank_branch text,
    terms_and_conditions text DEFAULT 'Thanks'::text,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    signature_image text,
    CONSTRAINT business_profile_id_check CHECK ((id = 1))
);


ALTER TABLE public.business_profile OWNER TO callintel;

--
-- Name: calls; Type: TABLE; Schema: public; Owner: callintel
--

CREATE TABLE public.calls (
    id integer NOT NULL,
    contact_id integer NOT NULL,
    direction text NOT NULL,
    outcome text NOT NULL,
    duration_sec integer,
    started_at timestamp with time zone DEFAULT now() NOT NULL,
    notes text,
    recording_url text,
    source text DEFAULT 'manual'::text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT calls_direction_check CHECK ((direction = ANY (ARRAY['incoming'::text, 'outgoing'::text]))),
    CONSTRAINT calls_outcome_check CHECK ((outcome = ANY (ARRAY['answered'::text, 'missed'::text, 'busy'::text]))),
    CONSTRAINT calls_source_check CHECK ((source = ANY (ARRAY['manual'::text, 'device'::text, 'telephony'::text])))
);


ALTER TABLE public.calls OWNER TO callintel;

--
-- Name: calls_id_seq; Type: SEQUENCE; Schema: public; Owner: callintel
--

CREATE SEQUENCE public.calls_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.calls_id_seq OWNER TO callintel;

--
-- Name: calls_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: callintel
--

ALTER SEQUENCE public.calls_id_seq OWNED BY public.calls.id;


--
-- Name: contacts; Type: TABLE; Schema: public; Owner: callintel
--

CREATE TABLE public.contacts (
    id integer NOT NULL,
    phone text NOT NULL,
    name text,
    city text,
    first_seen timestamp with time zone DEFAULT now() NOT NULL,
    last_contacted timestamp with time zone,
    gstin text,
    pan text,
    billing_address text,
    shipping_address text,
    state text,
    credit_limit numeric DEFAULT 0 NOT NULL
);


ALTER TABLE public.contacts OWNER TO callintel;

--
-- Name: contacts_id_seq; Type: SEQUENCE; Schema: public; Owner: callintel
--

CREATE SEQUENCE public.contacts_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.contacts_id_seq OWNER TO callintel;

--
-- Name: contacts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: callintel
--

ALTER SEQUENCE public.contacts_id_seq OWNED BY public.contacts.id;


--
-- Name: followups; Type: TABLE; Schema: public; Owner: callintel
--

CREATE TABLE public.followups (
    id integer NOT NULL,
    contact_id integer NOT NULL,
    call_id integer,
    due_at timestamp with time zone NOT NULL,
    reason text,
    status text DEFAULT 'pending'::text NOT NULL,
    channel text DEFAULT 'push'::text NOT NULL,
    sent_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    order_id integer,
    CONSTRAINT followups_channel_check CHECK ((channel = ANY (ARRAY['push'::text, 'whatsapp'::text]))),
    CONSTRAINT followups_status_check CHECK ((status = ANY (ARRAY['pending'::text, 'done'::text, 'cancelled'::text])))
);


ALTER TABLE public.followups OWNER TO callintel;

--
-- Name: followups_id_seq; Type: SEQUENCE; Schema: public; Owner: callintel
--

CREATE SEQUENCE public.followups_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.followups_id_seq OWNER TO callintel;

--
-- Name: followups_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: callintel
--

ALTER SEQUENCE public.followups_id_seq OWNED BY public.followups.id;


--
-- Name: order_items; Type: TABLE; Schema: public; Owner: callintel
--

CREATE TABLE public.order_items (
    id integer NOT NULL,
    order_id integer NOT NULL,
    product_name text NOT NULL,
    quantity numeric NOT NULL,
    unit_price numeric NOT NULL,
    gst_rate numeric,
    CONSTRAINT order_items_quantity_check CHECK ((quantity > (0)::numeric)),
    CONSTRAINT order_items_unit_price_check CHECK ((unit_price >= (0)::numeric))
);


ALTER TABLE public.order_items OWNER TO callintel;

--
-- Name: order_items_id_seq; Type: SEQUENCE; Schema: public; Owner: callintel
--

CREATE SEQUENCE public.order_items_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.order_items_id_seq OWNER TO callintel;

--
-- Name: order_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: callintel
--

ALTER SEQUENCE public.order_items_id_seq OWNED BY public.order_items.id;


--
-- Name: orders; Type: TABLE; Schema: public; Owner: callintel
--

CREATE TABLE public.orders (
    id integer NOT NULL,
    contact_id integer NOT NULL,
    call_id integer,
    order_number text NOT NULL,
    payment_status text DEFAULT 'pending'::text NOT NULL,
    fulfillment_status text DEFAULT 'not_ready'::text NOT NULL,
    notes text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    payment_method text,
    status text DEFAULT 'active'::text NOT NULL,
    CONSTRAINT orders_fulfillment_status_check CHECK ((fulfillment_status = ANY (ARRAY['not_ready'::text, 'ready'::text, 'dispatched'::text, 'delivered'::text]))),
    CONSTRAINT orders_payment_method_check CHECK (((payment_method IS NULL) OR (payment_method = ANY (ARRAY['cash'::text, 'upi'::text, 'card'::text, 'bank_transfer'::text, 'credit'::text, 'other'::text])))),
    CONSTRAINT orders_payment_status_check CHECK ((payment_status = ANY (ARRAY['pending'::text, 'paid'::text]))),
    CONSTRAINT orders_status_check CHECK ((status = ANY (ARRAY['active'::text, 'cancelled'::text])))
);


ALTER TABLE public.orders OWNER TO callintel;

--
-- Name: orders_id_seq; Type: SEQUENCE; Schema: public; Owner: callintel
--

CREATE SEQUENCE public.orders_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.orders_id_seq OWNER TO callintel;

--
-- Name: orders_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: callintel
--

ALTER SEQUENCE public.orders_id_seq OWNED BY public.orders.id;


--
-- Name: payments; Type: TABLE; Schema: public; Owner: callintel
--

CREATE TABLE public.payments (
    id integer NOT NULL,
    order_id integer NOT NULL,
    amount numeric NOT NULL,
    method text DEFAULT 'cash'::text NOT NULL,
    notes text,
    paid_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT payments_amount_check CHECK ((amount > (0)::numeric)),
    CONSTRAINT payments_method_check CHECK ((method = ANY (ARRAY['cash'::text, 'upi'::text, 'card'::text, 'bank_transfer'::text, 'other'::text])))
);


ALTER TABLE public.payments OWNER TO callintel;

--
-- Name: payments_id_seq; Type: SEQUENCE; Schema: public; Owner: callintel
--

CREATE SEQUENCE public.payments_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.payments_id_seq OWNER TO callintel;

--
-- Name: payments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: callintel
--

ALTER SEQUENCE public.payments_id_seq OWNED BY public.payments.id;


--
-- Name: products; Type: TABLE; Schema: public; Owner: callintel
--

CREATE TABLE public.products (
    id integer NOT NULL,
    name text NOT NULL,
    default_price numeric NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    hsn_code text,
    gst_rate numeric DEFAULT 18 NOT NULL,
    CONSTRAINT products_default_price_check CHECK ((default_price >= (0)::numeric))
);


ALTER TABLE public.products OWNER TO callintel;

--
-- Name: products_id_seq; Type: SEQUENCE; Schema: public; Owner: callintel
--

CREATE SEQUENCE public.products_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.products_id_seq OWNER TO callintel;

--
-- Name: products_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: callintel
--

ALTER SEQUENCE public.products_id_seq OWNED BY public.products.id;


--
-- Name: push_subscriptions; Type: TABLE; Schema: public; Owner: callintel
--

CREATE TABLE public.push_subscriptions (
    id integer NOT NULL,
    endpoint text NOT NULL,
    p256dh text NOT NULL,
    auth text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.push_subscriptions OWNER TO callintel;

--
-- Name: push_subscriptions_id_seq; Type: SEQUENCE; Schema: public; Owner: callintel
--

CREATE SEQUENCE public.push_subscriptions_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.push_subscriptions_id_seq OWNER TO callintel;

--
-- Name: push_subscriptions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: callintel
--

ALTER SEQUENCE public.push_subscriptions_id_seq OWNED BY public.push_subscriptions.id;


--
-- Name: quotation_items; Type: TABLE; Schema: public; Owner: callintel
--

CREATE TABLE public.quotation_items (
    id integer NOT NULL,
    quotation_id integer NOT NULL,
    product_name text NOT NULL,
    hsn_code text,
    unit text DEFAULT 'PCS'::text NOT NULL,
    quantity numeric NOT NULL,
    rate numeric NOT NULL,
    gst_rate numeric DEFAULT 18 NOT NULL,
    CONSTRAINT quotation_items_quantity_check CHECK ((quantity > (0)::numeric)),
    CONSTRAINT quotation_items_rate_check CHECK ((rate >= (0)::numeric))
);


ALTER TABLE public.quotation_items OWNER TO callintel;

--
-- Name: quotation_items_id_seq; Type: SEQUENCE; Schema: public; Owner: callintel
--

CREATE SEQUENCE public.quotation_items_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.quotation_items_id_seq OWNER TO callintel;

--
-- Name: quotation_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: callintel
--

ALTER SEQUENCE public.quotation_items_id_seq OWNED BY public.quotation_items.id;


--
-- Name: quotations; Type: TABLE; Schema: public; Owner: callintel
--

CREATE TABLE public.quotations (
    id integer NOT NULL,
    contact_id integer NOT NULL,
    quotation_date date DEFAULT CURRENT_DATE NOT NULL,
    bill_to_name text NOT NULL,
    bill_to_address text,
    bill_to_mobile text,
    bill_to_gstin text,
    bill_to_pan text,
    ship_to_name text,
    ship_to_address text,
    place_of_supply text,
    tax_mode text DEFAULT 'igst'::text NOT NULL,
    terms_and_conditions text,
    status text DEFAULT 'draft'::text NOT NULL,
    sent_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT quotations_status_check CHECK ((status = ANY (ARRAY['draft'::text, 'sent'::text]))),
    CONSTRAINT quotations_tax_mode_check CHECK ((tax_mode = ANY (ARRAY['igst'::text, 'split'::text])))
);


ALTER TABLE public.quotations OWNER TO callintel;

--
-- Name: quotations_id_seq; Type: SEQUENCE; Schema: public; Owner: callintel
--

CREATE SEQUENCE public.quotations_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.quotations_id_seq OWNER TO callintel;

--
-- Name: quotations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: callintel
--

ALTER SEQUENCE public.quotations_id_seq OWNED BY public.quotations.id;


--
-- Name: calls id; Type: DEFAULT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.calls ALTER COLUMN id SET DEFAULT nextval('public.calls_id_seq'::regclass);


--
-- Name: contacts id; Type: DEFAULT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.contacts ALTER COLUMN id SET DEFAULT nextval('public.contacts_id_seq'::regclass);


--
-- Name: followups id; Type: DEFAULT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.followups ALTER COLUMN id SET DEFAULT nextval('public.followups_id_seq'::regclass);


--
-- Name: order_items id; Type: DEFAULT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.order_items ALTER COLUMN id SET DEFAULT nextval('public.order_items_id_seq'::regclass);


--
-- Name: orders id; Type: DEFAULT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.orders ALTER COLUMN id SET DEFAULT nextval('public.orders_id_seq'::regclass);


--
-- Name: payments id; Type: DEFAULT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.payments ALTER COLUMN id SET DEFAULT nextval('public.payments_id_seq'::regclass);


--
-- Name: products id; Type: DEFAULT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.products ALTER COLUMN id SET DEFAULT nextval('public.products_id_seq'::regclass);


--
-- Name: push_subscriptions id; Type: DEFAULT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.push_subscriptions ALTER COLUMN id SET DEFAULT nextval('public.push_subscriptions_id_seq'::regclass);


--
-- Name: quotation_items id; Type: DEFAULT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.quotation_items ALTER COLUMN id SET DEFAULT nextval('public.quotation_items_id_seq'::regclass);


--
-- Name: quotations id; Type: DEFAULT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.quotations ALTER COLUMN id SET DEFAULT nextval('public.quotations_id_seq'::regclass);


--
-- Data for Name: business_profile; Type: TABLE DATA; Schema: public; Owner: callintel
--

COPY public.business_profile (id, name, address, gstin, pan, mobile, state, bank_name, bank_account_no, bank_ifsc, bank_branch, terms_and_conditions, updated_at, signature_image) FROM stdin;
1	Laduba Industries	Block No-80/A, City Survey No-NA0/A, Sheet No,NA99,Kamrej Kosmada, Surat, Surat, Gujarat, 395013	24DDPPK6180J1Z3	DDPPK6180J	7567714542	Gujarat	Laduba Industries	7567714542	KKBK0002860	Kotak Mahindra Bank, Kadodara Branch	Thanks	2026-08-29 14:31:00.056456+00	\N
\.


--
-- Data for Name: calls; Type: TABLE DATA; Schema: public; Owner: callintel
--

COPY public.calls (id, contact_id, direction, outcome, duration_sec, started_at, notes, recording_url, source, created_at) FROM stdin;
1	1	incoming	answered	180	2026-08-23 13:44:39.52827+00	Interested in a dining table set	\N	manual	2026-08-29 13:44:39.52827+00
2	1	outgoing	answered	90	2026-08-28 13:44:39.52827+00	Confirmed order details	\N	manual	2026-08-29 13:44:39.52827+00
3	2	incoming	answered	240	2026-08-21 13:44:39.52827+00	Bulk order inquiry for office furniture	\N	manual	2026-08-29 13:44:39.52827+00
4	2	incoming	answered	60	2026-08-27 13:44:39.52827+00	Placed a second order	\N	manual	2026-08-29 13:44:39.52827+00
5	3	incoming	answered	120	2026-08-27 13:44:39.52827+00	Wants a study table, will pay later	\N	manual	2026-08-29 13:44:39.52827+00
6	4	incoming	missed	\N	2026-08-25 13:44:39.52827+00	\N	\N	manual	2026-08-29 13:44:39.52827+00
7	4	incoming	missed	\N	2026-08-26 13:44:39.52827+00	\N	\N	manual	2026-08-29 13:44:39.52827+00
8	4	incoming	busy	\N	2026-08-28 13:44:39.52827+00	\N	\N	manual	2026-08-29 13:44:39.52827+00
9	5	incoming	answered	150	2026-08-29 11:44:39.52827+00	Interested in a bookshelf, call back today	\N	manual	2026-08-29 13:44:39.52827+00
10	6	incoming	answered	200	2026-08-23 13:44:39.52827+00	Asked about bed frame pricing	\N	manual	2026-08-29 13:44:39.52827+00
\.


--
-- Data for Name: contacts; Type: TABLE DATA; Schema: public; Owner: callintel
--

COPY public.contacts (id, phone, name, city, first_seen, last_contacted, gstin, pan, billing_address, shipping_address, state, credit_limit) FROM stdin;
4	+919876543213	Meera Shah	Surat	2026-08-25 13:44:39.527552+00	2026-08-28 13:44:39.527552+00	\N	\N	\N	\N	\N	0
5	+919876543214	Arjun Rao	Bangalore	2026-08-26 13:44:39.527552+00	2026-08-29 11:44:39.527552+00	\N	\N	\N	\N	\N	0
6	+919876543215	Priya Nair	Kochi	2026-08-23 13:44:39.527552+00	2026-08-23 13:44:39.527552+00	\N	\N	\N	\N	\N	0
7	+919854304345	Jagriti Industries	\N	2026-08-29 14:04:20.195501+00	\N	18AADFJ6460G1ZB	AADFJ6460G	01, Fatasil Old Glass Factory Complex, Guwahati, Kamrup Metropolitan, Assam, 781009	\N	Assam	0
8	+919000011111	Ahmedabad Traders	\N	2026-08-29 14:04:57.474403+00	\N	\N	\N	\N	\N	Gujarat	0
9	+919000022222	GST Test Co	\N	2026-08-29 14:04:57.545954+00	\N	27ABCDE1234F1Z5	ABCDE1234F	\N	\N	Maharashtra	0
3	+919876543212	Vivek Patel	Ahmedabad	2026-08-27 13:44:39.527552+00	2026-08-27 13:44:39.527552+00	24AAAAA0000A1Z5	\N	\N	\N	Gujarat	0
10	+919000099999	GST Order Test	\N	2026-08-29 14:24:09.564626+00	\N	\N	\N	\N	\N	\N	0
11	+919812345678	Bhavya Enterprises	\N	2026-08-29 14:34:03.409691+00	\N	24AABCB1234C1Z8	AABCB1234C	14, GIDC Industrial Estate, Vatva, Ahmedabad, Gujarat, 382445	\N	Gujarat	0
2	+919876543211	Sunita Traders	Mumbai	2026-08-21 13:44:39.527552+00	2026-08-27 13:44:39.527552+00	27AABCS4321M1Z9	AABCS4321M	Shop 4, Lohar Chawl, Kalbadevi, Mumbai, Maharashtra, 400002	Warehouse 7, MIDC Andheri, Mumbai, Maharashtra, 400093	Maharashtra	0
12	+919845098450	Chennai Textiles Pvt Ltd	\N	2026-08-29 14:34:03.469717+00	\N	33AACCC5678D1ZQ	AACCC5678D	221, Anna Salai, T. Nagar, Chennai, Tamil Nadu, 600017	\N	Tamil Nadu	0
1	+919876543210	Rajesh Kumar	Pune	2026-08-23 13:44:39.527552+00	2026-08-28 13:44:39.527552+00	27AAAPR5678K1Z2	AAAPR5678K	12, Shivaji Nagar, Pune, Maharashtra, 411005	12, Shivaji Nagar, Pune, Maharashtra, 411005	Maharashtra	0
13	9510099905	Trushant 	Surat	2026-09-12 05:28:10.359494+00	\N	\N	\N	\N	\N	\N	0
\.


--
-- Data for Name: followups; Type: TABLE DATA; Schema: public; Owner: callintel
--

COPY public.followups (id, contact_id, call_id, due_at, reason, status, channel, sent_at, created_at, order_id) FROM stdin;
5	1	1	2026-08-24 13:44:39.531115+00	Confirm dining table interest	done	push	2026-08-24 13:44:39.531115+00	2026-08-29 13:44:39.531115+00	\N
4	2	4	2026-08-27 13:44:39.531115+00	Follow up on bulk order satisfaction	pending	whatsapp	2026-08-29 13:45:00.032085+00	2026-08-29 13:44:39.531115+00	\N
3	5	9	2026-08-29 18:00:00+00	Call back about bookshelf	pending	push	2026-08-29 18:45:34.43738+00	2026-08-29 13:44:39.531115+00	\N
2	3	5	2026-08-30 13:44:39.531115+00	Collect payment for order ORD-00006	pending	push	2026-08-30 19:09:00.041961+00	2026-08-29 13:44:39.531115+00	6
7	9	\N	2026-08-30 14:04:57.544+00	Collect payment for order ORD-00007	pending	push	2026-08-30 19:09:00.051037+00	2026-08-29 14:04:57.545954+00	7
8	10	\N	2026-08-30 14:24:09.578+00	Collect payment for order ORD-00008	pending	push	2026-08-30 19:09:00.057586+00	2026-08-29 14:24:09.564626+00	8
6	4	8	2026-08-31 13:44:39.531115+00	Try calling again — missed 3 times	pending	push	2026-08-31 14:02:01.006014+00	2026-08-29 13:44:39.531115+00	\N
1	1	2	2026-09-01 13:44:39.531115+00	Check satisfaction after delivery	pending	push	2026-09-01 13:45:00.092365+00	2026-08-29 13:44:39.531115+00	\N
9	1	\N	2026-09-13 05:23:54.89+00	Collect payment for order ORD-00009	pending	push	\N	2026-09-12 05:23:54.939327+00	\N
10	1	\N	2026-09-13 05:24:01.805+00	Collect payment for order ORD-00010	pending	push	\N	2026-09-12 05:24:01.869697+00	\N
11	1	\N	2026-09-13 05:24:08.383+00	Collect payment for order ORD-00011	pending	push	\N	2026-09-12 05:24:08.444591+00	\N
12	13	\N	2026-09-13 05:28:10.309+00	Collect payment for order ORD-00012	cancelled	push	\N	2026-09-12 05:28:10.359494+00	12
13	13	\N	2026-09-13 05:38:02.289+00	Collect payment for order ORD-00013	pending	push	\N	2026-09-12 05:38:02.348134+00	13
\.


--
-- Data for Name: order_items; Type: TABLE DATA; Schema: public; Owner: callintel
--

COPY public.order_items (id, order_id, product_name, quantity, unit_price, gst_rate) FROM stdin;
1	1	Dining Table	1	8500	\N
2	2	High Table	2	4200	\N
3	3	Study Table	1	3200	\N
4	4	Office Chair	10	2500	\N
5	4	Bookshelf	4	3800	\N
6	5	Bed Frame (Queen)	1	12000	\N
7	6	Study Table	1	3200	\N
8	7	Study Table	1	3200	\N
9	8	Test GST Product	2	1000	18
13	12	Dining Table	1	8500	18
14	12	High Table	2	4200	18
15	13	Bed Frame (Queen)	1	12000	18
\.


--
-- Data for Name: orders; Type: TABLE DATA; Schema: public; Owner: callintel
--

COPY public.orders (id, contact_id, call_id, order_number, payment_status, fulfillment_status, notes, created_at, updated_at, payment_method, status) FROM stdin;
1	1	2	ORD-00001	paid	dispatched	\N	2026-08-29 13:44:39.529347+00	2026-08-29 13:44:39.529347+00	upi	active
2	1	\N	ORD-00002	paid	delivered	\N	2026-08-29 13:44:39.529347+00	2026-08-29 13:44:39.529347+00	cash	active
3	1	\N	ORD-00003	paid	ready	\N	2026-08-29 13:44:39.529347+00	2026-08-29 13:44:39.529347+00	card	active
4	2	4	ORD-00004	paid	not_ready	Bulk office order	2026-08-29 13:44:39.529347+00	2026-08-29 13:44:39.529347+00	bank_transfer	active
5	2	\N	ORD-00005	pending	not_ready	Customer changed their mind	2026-08-29 13:44:39.529347+00	2026-08-29 13:44:39.529347+00	\N	cancelled
7	9	\N	ORD-00007	pending	not_ready	\N	2026-08-29 14:04:57.545954+00	2026-08-29 14:04:57.545954+00	\N	active
12	13	\N	ORD-00012	paid	not_ready	\N	2026-09-12 05:28:10.359494+00	2026-09-12 05:28:36.012808+00	credit	active
8	10	\N	ORD-00008	pending	not_ready	\N	2026-08-29 14:24:09.564626+00	2026-09-12 05:32:11.736321+00	\N	active
6	3	5	ORD-00006	pending	not_ready	\N	2026-08-29 13:44:39.529347+00	2026-09-12 05:32:23.492581+00	\N	active
13	13	\N	ORD-00013	pending	not_ready	\N	2026-09-12 05:38:02.348134+00	2026-09-12 05:38:02.348134+00	credit	active
\.


--
-- Data for Name: payments; Type: TABLE DATA; Schema: public; Owner: callintel
--

COPY public.payments (id, order_id, amount, method, notes, paid_at) FROM stdin;
2	12	16900	bank_transfer	aapi didha	2026-09-12 05:28:36.012808+00
\.


--
-- Data for Name: products; Type: TABLE DATA; Schema: public; Owner: callintel
--

COPY public.products (id, name, default_price, created_at, hsn_code, gst_rate) FROM stdin;
1	Dining Table	8500	2026-08-29 13:44:39.526654+00	\N	18
2	High Table	4200	2026-08-29 13:44:39.526654+00	\N	18
3	Study Table	3200	2026-08-29 13:44:39.526654+00	\N	18
4	Office Chair	2500	2026-08-29 13:44:39.526654+00	\N	18
5	Bookshelf	3800	2026-08-29 13:44:39.526654+00	\N	18
6	Bed Frame (Queen)	12000	2026-08-29 13:44:39.526654+00	\N	18
7	Test GST Product	1000	2026-08-29 14:24:09.501564+00	9403	18
\.


--
-- Data for Name: push_subscriptions; Type: TABLE DATA; Schema: public; Owner: callintel
--

COPY public.push_subscriptions (id, endpoint, p256dh, auth, created_at) FROM stdin;
\.


--
-- Data for Name: quotation_items; Type: TABLE DATA; Schema: public; Owner: callintel
--

COPY public.quotation_items (id, quotation_id, product_name, hsn_code, unit, quantity, rate, gst_rate) FROM stdin;
1	1	C Bending Digital Laptop Table	9403	PCS	150	165.25	18
2	2	Office Chair	9401	PCS	5	2500	18
3	3	Study Table	9403	PCS	10	3200	18
4	4	Office Chair	9401	PCS	20	2500	18
5	4	Bookshelf	9403	PCS	5	3800	18
6	5	Bookshelf	\N	PCS	2	3800	18
7	5	Office Chair	\N	PCS	10	2500	18
\.


--
-- Data for Name: quotations; Type: TABLE DATA; Schema: public; Owner: callintel
--

COPY public.quotations (id, contact_id, quotation_date, bill_to_name, bill_to_address, bill_to_mobile, bill_to_gstin, bill_to_pan, ship_to_name, ship_to_address, place_of_supply, tax_mode, terms_and_conditions, status, sent_at, created_at) FROM stdin;
2	8	2026-08-29	Ahmedabad Traders	\N	+919000011111	\N	\N	Ahmedabad Traders	\N	Gujarat	split	Thanks	draft	\N	2026-08-29 14:04:57.474403+00
3	11	2026-08-29	Bhavya Enterprises	14, GIDC Industrial Estate, Vatva, Ahmedabad, Gujarat, 382445	+919812345678	24AABCB1234C1Z8	AABCB1234C	Bhavya Enterprises	14, GIDC Industrial Estate, Vatva, Ahmedabad, Gujarat, 382445	Gujarat	split	Thanks	draft	\N	2026-08-29 14:34:03.409691+00
4	12	2026-08-29	Chennai Textiles Pvt Ltd	221, Anna Salai, T. Nagar, Chennai, Tamil Nadu, 600017	+919845098450	33AACCC5678D1ZQ	AACCC5678D	Chennai Textiles Pvt Ltd	221, Anna Salai, T. Nagar, Chennai, Tamil Nadu, 600017	Tamil Nadu	igst	Thanks	draft	\N	2026-08-29 14:34:03.469717+00
1	7	2026-08-29	Jagriti Industries	01, Fatasil Old Glass Factory Complex, Guwahati, Kamrup Metropolitan, Assam, 781009	+919854304345	18AADFJ6460G1ZB	AADFJ6460G	Jagriti Industries	01, Fatasil Old Glass Factory Complex, Guwahati, Kamrup Metropolitan, Assam, 781009	Assam	igst	Thanks	sent	2026-09-02 08:38:29.720799+00	2026-08-29 14:04:20.195501+00
5	12	2026-09-02	Chennai Textiles Pvt Ltd	221, Anna Salai, T. Nagar, Chennai, Tamil Nadu, 600017	+919845098450	33AACCC5678D1ZQ	AACCC5678D	Chennai Textiles Pvt Ltd	221, Anna Salai, T. Nagar, Chennai, Tamil Nadu, 600017	Tamil Nadu	igst	Thanks	draft	\N	2026-09-02 09:20:57.22745+00
\.


--
-- Name: calls_id_seq; Type: SEQUENCE SET; Schema: public; Owner: callintel
--

SELECT pg_catalog.setval('public.calls_id_seq', 10, true);


--
-- Name: contacts_id_seq; Type: SEQUENCE SET; Schema: public; Owner: callintel
--

SELECT pg_catalog.setval('public.contacts_id_seq', 13, true);


--
-- Name: followups_id_seq; Type: SEQUENCE SET; Schema: public; Owner: callintel
--

SELECT pg_catalog.setval('public.followups_id_seq', 13, true);


--
-- Name: order_items_id_seq; Type: SEQUENCE SET; Schema: public; Owner: callintel
--

SELECT pg_catalog.setval('public.order_items_id_seq', 15, true);


--
-- Name: orders_id_seq; Type: SEQUENCE SET; Schema: public; Owner: callintel
--

SELECT pg_catalog.setval('public.orders_id_seq', 13, true);


--
-- Name: payments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: callintel
--

SELECT pg_catalog.setval('public.payments_id_seq', 2, true);


--
-- Name: products_id_seq; Type: SEQUENCE SET; Schema: public; Owner: callintel
--

SELECT pg_catalog.setval('public.products_id_seq', 7, true);


--
-- Name: push_subscriptions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: callintel
--

SELECT pg_catalog.setval('public.push_subscriptions_id_seq', 1, false);


--
-- Name: quotation_items_id_seq; Type: SEQUENCE SET; Schema: public; Owner: callintel
--

SELECT pg_catalog.setval('public.quotation_items_id_seq', 7, true);


--
-- Name: quotations_id_seq; Type: SEQUENCE SET; Schema: public; Owner: callintel
--

SELECT pg_catalog.setval('public.quotations_id_seq', 5, true);


--
-- Name: business_profile business_profile_pkey; Type: CONSTRAINT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.business_profile
    ADD CONSTRAINT business_profile_pkey PRIMARY KEY (id);


--
-- Name: calls calls_pkey; Type: CONSTRAINT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.calls
    ADD CONSTRAINT calls_pkey PRIMARY KEY (id);


--
-- Name: contacts contacts_phone_key; Type: CONSTRAINT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.contacts
    ADD CONSTRAINT contacts_phone_key UNIQUE (phone);


--
-- Name: contacts contacts_pkey; Type: CONSTRAINT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.contacts
    ADD CONSTRAINT contacts_pkey PRIMARY KEY (id);


--
-- Name: followups followups_pkey; Type: CONSTRAINT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.followups
    ADD CONSTRAINT followups_pkey PRIMARY KEY (id);


--
-- Name: order_items order_items_pkey; Type: CONSTRAINT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT order_items_pkey PRIMARY KEY (id);


--
-- Name: orders orders_order_number_key; Type: CONSTRAINT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_order_number_key UNIQUE (order_number);


--
-- Name: orders orders_pkey; Type: CONSTRAINT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id);


--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- Name: products products_name_key; Type: CONSTRAINT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_name_key UNIQUE (name);


--
-- Name: products products_pkey; Type: CONSTRAINT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_pkey PRIMARY KEY (id);


--
-- Name: push_subscriptions push_subscriptions_endpoint_key; Type: CONSTRAINT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.push_subscriptions
    ADD CONSTRAINT push_subscriptions_endpoint_key UNIQUE (endpoint);


--
-- Name: push_subscriptions push_subscriptions_pkey; Type: CONSTRAINT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.push_subscriptions
    ADD CONSTRAINT push_subscriptions_pkey PRIMARY KEY (id);


--
-- Name: quotation_items quotation_items_pkey; Type: CONSTRAINT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.quotation_items
    ADD CONSTRAINT quotation_items_pkey PRIMARY KEY (id);


--
-- Name: quotations quotations_pkey; Type: CONSTRAINT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.quotations
    ADD CONSTRAINT quotations_pkey PRIMARY KEY (id);


--
-- Name: idx_calls_contact_id; Type: INDEX; Schema: public; Owner: callintel
--

CREATE INDEX idx_calls_contact_id ON public.calls USING btree (contact_id);


--
-- Name: idx_followups_contact_id; Type: INDEX; Schema: public; Owner: callintel
--

CREATE INDEX idx_followups_contact_id ON public.followups USING btree (contact_id);


--
-- Name: idx_followups_due_at; Type: INDEX; Schema: public; Owner: callintel
--

CREATE INDEX idx_followups_due_at ON public.followups USING btree (due_at) WHERE (status = 'pending'::text);


--
-- Name: idx_followups_order_id; Type: INDEX; Schema: public; Owner: callintel
--

CREATE INDEX idx_followups_order_id ON public.followups USING btree (order_id);


--
-- Name: idx_order_items_order_id; Type: INDEX; Schema: public; Owner: callintel
--

CREATE INDEX idx_order_items_order_id ON public.order_items USING btree (order_id);


--
-- Name: idx_orders_contact_id; Type: INDEX; Schema: public; Owner: callintel
--

CREATE INDEX idx_orders_contact_id ON public.orders USING btree (contact_id);


--
-- Name: idx_payments_order_id; Type: INDEX; Schema: public; Owner: callintel
--

CREATE INDEX idx_payments_order_id ON public.payments USING btree (order_id);


--
-- Name: idx_quotation_items_quotation_id; Type: INDEX; Schema: public; Owner: callintel
--

CREATE INDEX idx_quotation_items_quotation_id ON public.quotation_items USING btree (quotation_id);


--
-- Name: idx_quotations_contact_id; Type: INDEX; Schema: public; Owner: callintel
--

CREATE INDEX idx_quotations_contact_id ON public.quotations USING btree (contact_id);


--
-- Name: calls calls_contact_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.calls
    ADD CONSTRAINT calls_contact_id_fkey FOREIGN KEY (contact_id) REFERENCES public.contacts(id) ON DELETE CASCADE;


--
-- Name: followups followups_call_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.followups
    ADD CONSTRAINT followups_call_id_fkey FOREIGN KEY (call_id) REFERENCES public.calls(id) ON DELETE SET NULL;


--
-- Name: followups followups_contact_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.followups
    ADD CONSTRAINT followups_contact_id_fkey FOREIGN KEY (contact_id) REFERENCES public.contacts(id) ON DELETE CASCADE;


--
-- Name: followups followups_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.followups
    ADD CONSTRAINT followups_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE SET NULL;


--
-- Name: order_items order_items_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT order_items_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE CASCADE;


--
-- Name: orders orders_call_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_call_id_fkey FOREIGN KEY (call_id) REFERENCES public.calls(id) ON DELETE SET NULL;


--
-- Name: orders orders_contact_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_contact_id_fkey FOREIGN KEY (contact_id) REFERENCES public.contacts(id) ON DELETE CASCADE;


--
-- Name: payments payments_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE CASCADE;


--
-- Name: quotation_items quotation_items_quotation_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.quotation_items
    ADD CONSTRAINT quotation_items_quotation_id_fkey FOREIGN KEY (quotation_id) REFERENCES public.quotations(id) ON DELETE CASCADE;


--
-- Name: quotations quotations_contact_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: callintel
--

ALTER TABLE ONLY public.quotations
    ADD CONSTRAINT quotations_contact_id_fkey FOREIGN KEY (contact_id) REFERENCES public.contacts(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict pvGwKoBRxlXQ9JYzYfrmphPgSKD577nUSLdlp1Dd5aqXIxbn607sU1Oew9Wh76W

