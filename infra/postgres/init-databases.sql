-- Script executado pelo PostgreSQL na primeira inicialização do container
-- Cria os 9 bancos de dados — um por bounded context

-- pascoa_monolith já é criado via POSTGRES_DB no docker-compose

CREATE DATABASE pascoa_auth;
CREATE DATABASE pascoa_customers;
CREATE DATABASE pascoa_inventory;
CREATE DATABASE pascoa_products;
CREATE DATABASE pascoa_orders;
CREATE DATABASE pascoa_production;
CREATE DATABASE pascoa_financial;
CREATE DATABASE pascoa_notifications;
CREATE DATABASE pascoa_analytics;

-- Garante que o usuário postgres tem acesso a todos os bancos
GRANT ALL PRIVILEGES ON DATABASE pascoa_monolith    TO postgres;
GRANT ALL PRIVILEGES ON DATABASE pascoa_auth        TO postgres;
GRANT ALL PRIVILEGES ON DATABASE pascoa_customers   TO postgres;
GRANT ALL PRIVILEGES ON DATABASE pascoa_inventory   TO postgres;
GRANT ALL PRIVILEGES ON DATABASE pascoa_products    TO postgres;
GRANT ALL PRIVILEGES ON DATABASE pascoa_orders      TO postgres;
GRANT ALL PRIVILEGES ON DATABASE pascoa_production  TO postgres;
GRANT ALL PRIVILEGES ON DATABASE pascoa_financial   TO postgres;
GRANT ALL PRIVILEGES ON DATABASE pascoa_notifications TO postgres;
GRANT ALL PRIVILEGES ON DATABASE pascoa_analytics   TO postgres;
