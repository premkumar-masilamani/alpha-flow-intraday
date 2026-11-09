# Gracefully handle missing local environment configurations in CI/CD
-include .env
ifneq ($(wildcard .env),)
    export $(shell sed 's/=.*//' .env)
endif

.PHONY: all run_database connect_database run_backend run_frontend test lint

all: run_backend

run_database:
	@echo "Starting the database on port $(POSTGRES_PORT)..."
	@$(MAKE) -C database all

connect_database:
	@echo "Connecting to the database CLI..."
	@$(MAKE) -C database connect_database

run_backend:
	@echo "Launching backend development server..."
	@$(MAKE) -C backend dev

run_frontend:
	@echo "Launching frontend development server..."
	@$(MAKE) -C frontend dev

test:
	@echo "Running backend test suites..."
	@$(MAKE) -C backend test
	@echo "Running frontend test suites..."
	@$(MAKE) -C frontend test

lint:
	@echo "Linting backend source code..."
	@$(MAKE) -C backend lint
	@echo "Linting frontend source code..."
	@$(MAKE) -C frontend lint
