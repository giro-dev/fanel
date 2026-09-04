.PHONY: dev-backend dev-frontend build test docker-build

dev-backend:
	mvn -pl backend spring-boot:run -Dskip.frontend -Dspring-boot.run.profiles=dev,sqlite

dev-frontend:
	cd frontend && npm run dev

build:
	mvn -B -ntp package

test:
	mvn -B -ntp verify

docker-build:
	docker build -f deploy/Dockerfile .
