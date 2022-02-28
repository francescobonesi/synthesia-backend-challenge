.PHONY: build run clean

build:
	docker build -t francesco/app .

run:
	docker-compose up -d --remove-orphans

configure:
	curl -i -u guest:guest -H "content-type:application/json" -X PUT http://localhost:15672/api/queues/%2f/requests -d '{"auto_delete":false,"durable":true,"arguments":{}}'
	curl -i -u guest:guest -H "content-type:application/json" -X PUT http://localhost:15672/api/queues/%2f/signatures -d '{"auto_delete":false,"durable":true,"arguments":{}}'

clean:
	docker-compose down --remove-orphans
