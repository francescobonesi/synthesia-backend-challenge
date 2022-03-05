.PHONY: compile build-react-image build-job-image build-api-image build run run-background configure clean performance-test same-message-many-times

java_home ?= ~/.sdkman/candidates/java/17.0.2-zulu/
synthesia_key ?= not_valid_key_by_default

compile:
	JAVA_HOME=$(java_home) ./gradlew clean build

build-api-image:
	cd synthesia-api && docker build -t francesco/api .

build-job-image:
	cd synthesia-job && docker build -t francesco/job .

build-react-image:
	cd waiting-page && docker build -t francesco/waiting .

build: build-api-image build-job-image build-react-image

init:
	docker-compose up -d --remove-orphans

configure:
	curl -i -u guest:guest -H "content-type:application/json" -X PUT http://localhost:15672/api/queues/%2f/requests -d '{"auto_delete":false,"durable":true,"arguments":{}}'
	curl -i -u guest:guest -H "content-type:application/json" -X PUT http://localhost:15672/api/queues/%2f/signatures -d '{"auto_delete":false,"durable":true,"arguments":{}}'

run:
	docker run -p "8080:8080" --network=host francesco/api & docker run --network=host -e SYNTHESIA_KEY=$(synthesia_key) francesco/job

run-background:
	docker run -d -p "8080:8080" --network=host francesco/api
	docker run -d --network=host -e SYNTHESIA_KEY=$(synthesia_key)  francesco/job

clean:
	docker-compose down --remove-orphans

performance-test:
	cd artillery && artillery run config.yml -o output.json && artillery report output.json

same-message-many-times:
	cd artillery && artillery run single-message-many-times.yml -o smmt-output.json && artillery report smmt-output.json