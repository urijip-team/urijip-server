#!/bin/sh
# LocalStack이 준비되면 실행된다. 로컬 실행(compose.yaml)과 테스트(Testcontainers)가 함께 쓴다.
awslocal s3 mb s3://urijip-local
