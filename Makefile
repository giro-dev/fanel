.PHONY: dev-backend dev-frontend build test docker-build android-build android-test \
	docs-sync docs-openapi docs-build docs-serve

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

android-build:
	cd android && ./gradlew assembleDebug

android-test:
	cd android && ./gradlew testDebugUnitTest

# Documentation site (docs-site/, Hugo + Docsy) -------------------------------

docs-sync:
	./scripts/sync-docs.sh

docs-openapi:
	./scripts/generate-openapi.sh

docs-build: docs-sync
	cd docs-site && npm install && hugo --minify

docs-serve: docs-sync
	cd docs-site && npm install && hugo server
