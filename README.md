
## OSE 3.x setup

* You might want to modify settings.xml and/or assemble script in each project to reflect your environment. Actually settings.xml uses remote repos, and settings_local.xml is example of using local repo.

* oadm new-project microservices --display-name='Microservices application' --description='Web shop application based on microservices architecture' --node-selector='region=primary'

* oc project microservices

* This is optional: 

oc policy add-role-to-user edit demouser -n $(oc project -q)

* oc create -f https://raw.githubusercontent.com/jboss-openshift/application-templates/master/secrets/eap-app-secret.json

* oc policy add-role-to-user view system:serviceaccount:$(oc project -q):default -n $(oc project -q)

* oc policy add-role-to-user view system:serviceaccount:$(oc project -q):eap-service-account -n $(oc project -q)

* create 3 persistent volumes for sales database, product database and amq

create pvol.json

```{
  "apiVersion": "v1",
  "kind": "PersistentVolume",
  "metadata": {
    "name": "pv0001"
  },
  "spec": {
    "capacity": {
        "storage": "128Mi"
        },
    "accessModes": [ "ReadWriteOnce" ],
    "nfs": {
        "path": "<path on the server>",
        "server": "<server hostname>"
    },
    "persistentVolumeReclaimPolicy": "Recycle"
  }
}```

* oc create -f https://raw.githubusercontent.com/jstakun/microservices/master/product-template.json

* oc new-app product-template

* oc rsh --shell '/bin/bash' product-mysql-1-tptj5

* mysql -u root -D product

* create Product, Keyword and PRODUCT_KEYWORD tables and insert respective records using https://raw.githubusercontent.com/jstakun/microservices/master/setup_microservices.sql

* oc create -f https://raw.githubusercontent.com/jstakun/microservices/master/sales-template.json

* oc new-app sales-template

* oc rsh --shell '/bin/bash' sales-mysql-1-nceeh

* mysql -u root -D sales

* create Customer, Orders and OrderItem tables using https://raw.githubusercontent.com/jstakun/microservices/master/setup_microservices.sql

* oc create -f https://raw.githubusercontent.com/jstakun/microservices/master/billing-template.json

* oc new-app billing-template

* oc create -f https://raw.githubusercontent.com/jboss-openshift/application-templates/master/secrets/amq-app-secret.json

* oc new-app amq62-persistent -p VOLUME_CAPACITY=128Mi,MQ_USERNAME=admin,MQ_PASSWORD=manager1,MQ_QUEUES=transactions,products,customers,orders  

* oc create -f https://raw.githubusercontent.com/jstakun/microservices/master/eventbus2/eventbus2-template.json

* oc new-app eventbus2 -p GIT_REPO=https://github.com/jstakun/eventbus2,AMQ_HOST=172.30.248.83

* oc create -f https://raw.githubusercontent.com/jstakun/microservices/master/presentation-template.json

* #get default token

oc get -n microservices sa/default --template='{{range .secrets}}{{printf "%s\n" .name}}{{end}}'

* #use default token name from below  

oc new-app presentation-template -p API_TOKEN=$(oc get -n microservices secrets default-token-xqxmg --template='{{.data.token}}' | base64 -d)

* This is optional:

You could create auto scaler for shop pod

create scaler.yaml

```
apiVersion: extensions/v1beta1
kind: HorizontalPodAutoscaler
metadata:
  name: shop-scaler 
spec:
  scaleRef:
    kind: DeploymentConfig 
    name: shop 
    apiVersion: v1 
    subresource: scale
  minReplicas: 1 
  maxReplicas: 2
  cpuUtilization:
    targetPercentage: 80 
```

oc create -f scaler.yaml


