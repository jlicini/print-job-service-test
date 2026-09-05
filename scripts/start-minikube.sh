#!/bin/sh
cd ..

minikube start

docker build -t print-job-service:latest .

minikube image load print-job-service:latest

kubectl apply -f postgres-storage.yml
kubectl apply -f postgres-deployment.yml
kubectl apply -f postgres-service.yml
kubectl apply -f app-deployment.yml
kubectl apply -f app-service.yml

kubectl get pods
kubectl get services