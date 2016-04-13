OSE 3.1 setup 

-------------------------------------------

1. You might want to modify settings.xml and/or assemble script in each project to reflect your environment

2. oc new-project microservices --display-name='Microservices application' --description='Web sales application based on microservices architecture'

3. oc create -f https://raw.githubusercontent.com/jboss-openshift/application-templates/master/secrets/eap-app-secret.json

4. oc policy add-role-to-user view system:serviceaccount:$(oc project -q):default -n $(oc project -q)

5. oc policy add-role-to-user view system:serviceaccount:$(oc project -q):eap-service-account -n $(oc project -q)

6. create 3 persistent volumes for sales database, product database and amq

{
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
}

7. oc new-app eap64-mysql-persistent-s2i \
    -p APPLICATION_NAME=product,SOURCE_REPOSITORY_URL=https://github.com/jstakun/microservices,SOURCE_REPOSITORY_REF=master,CONTEXT_DIR=product,DB_JNDI=java:jboss/datasources/ProductDS,DB_DATABASE=product,VOLUME_CAPACITY=128Mi,HTTPS_NAME=jboss,HTTPS_PASSWORD=mykeystorepass

8. oc rsh --shell '/bin/bash' product-mysql-1-qm0jw

mysql -u root -D product

create Product, Keyword and PRODUCT_KEYWORD tables and insert respective records using setup_microservices.sql. Don't create database!

9. This is optional:

oc edit dc product -o json

"strategy": {
            "type": "Rolling",
            "rollingParams": {
                "updatePeriodSeconds": 1,
                "intervalSeconds": 1,
                "timeoutSeconds": 600,
                "maxUnavailable": "25%",
                "maxSurge": "25%"
            },
            "resources": {}
},

"livenessProbe": {
            "httpGet": {
                 "path": "/_status/dbhealthz",
                 "port": 8080,
                 "scheme": "HTTP"
            },
            "initialDelaySeconds": 120,
            "timeoutSeconds": 10
},

10. This is optional: 

oc env dc product "JAVA_OPTS=-Xmx512m -XX:MaxPermSize=256m -Djava.net.preferIPv4Stack=true -Djboss.modules.system.pkgs=org.jboss.logmanager -Djava.awt.headless=true -Djboss.modules.policy-permissions=true"

11. oc new-app eap64-mysql-persistent-s2i \
    -p APPLICATION_NAME=sales,SOURCE_REPOSITORY_URL=https://github.com/jstakun/microservices,SOURCE_REPOSITORY_REF=master,CONTEXT_DIR=sales,DB_JNDI=java:jboss/datasources/SalesDS,DB_DATABASE=sales,VOLUME_CAPACITY=128Mi,HTTPS_NAME=jboss,HTTPS_PASSWORD=mykeystorepass

12. oc rsh --shell '/bin/bash' sales-mysql-1-qm0jw

mysql -u root -D sales

create Customer, Orders and OrderItem tables using setup_microservices.sql

13. This is optional: 

oc edit dc sales -o json

"livenessProbe": {
        "httpGet": {
                  "path": "/_status/dbhealthz",
                  "port": 8080,
                  "scheme": "HTTP"
        },
        "initialDelaySeconds": 120,
        "timeoutSeconds": 10
},

14. This is optional: 

oc env dc sales "JAVA_OPTS=-Xmx512m -XX:MaxPermSize=256m -Djava.net.preferIPv4Stack=true -Djboss.modules.system.pkgs=org.jboss.logmanager -Djava.awt.headless=true -Djboss.modules.policy-permissions=true"

15: oc new-app eap64-basic-s2i \
    -p APPLICATION_NAME=billing,SOURCE_REPOSITORY_URL=https://github.com/jstakun/microservices,SOURCE_REPOSITORY_REF=master,CONTEXT_DIR=billing

16. This is optional:

oc env dc billing "JAVA_OPTS=-Xmx512m -XX:MaxPermSize=256m -Djava.net.preferIPv4Stack=true -Djboss.modules.system.pkgs=org.jboss.logmanager -Djava.awt.headless=true -Djboss.modules.policy-permissions=true"

17. oc new-app eap64-basic-s2i \
    -p APPLICATION_NAME=shop,SOURCE_REPOSITORY_URL=https://github.com/jstakun/microservices,SOURCE_REPOSITORY_REF=master,CONTEXT_DIR=presentation

18. oc env dc shop API_TOKEN=$(oc whoami -t) JAVA_OPTS="-Xmx512m -XX:MaxPermSize=256m -Djava.net.preferIPv4Stack=true -Djboss.modules.system.pkgs=org.jboss.logmanager -Djava.awt.headless=true -Djboss.modules.policy-permissions=true"

19. oc create -f https://raw.githubusercontent.com/jboss-openshift/application-templates/master/secrets/amq-app-secret.json

20. oc new-app amq62-persistent -p VOLUME_CAPACITY=128Mi,MQ_USERNAME=admin,MQ_PASSWORD=manager1,MQ_QUEUES=transactions,products,customers,orders  

21. oc create -f https://raw.githubusercontent.com/jstakun/eventbus2/master/eventbus2-template.json

22. oc new-app eventbus2 -p GIT_REPO=https://github.com/jstakun/eventbus2

23. AMQ_HOST should point to broker-amq-tcp service IP address

oc env dc eventbus2 AMQ_HOST=172.30.20.199

24. This is optional:

You could create auto scaller for shop pod

create scaler.yaml

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

oc create -f scaler.yaml


