Hello Kafka Salesforce
----------------------

    PushTopic pushTopic = new PushTopic();
    pushTopic.Name = 'ContactUpdates';
    pushTopic.Query = 'SELECT Id, Name FROM Contact';
    pushTopic.ApiVersion = 36.0;
    pushTopic.NotifyForOperationCreate = true;
    pushTopic.NotifyForOperationUpdate = true;
    pushTopic.NotifyForOperationUndelete = true;
    pushTopic.NotifyForOperationDelete = true;
    pushTopic.NotifyForFields = 'Referenced';
    insert pushTopic;

    heroku config -s > .env
    set -o allexport
    source .env
    set +o allexport

    bin/setup_certs.sh
    
    ./activator ~run