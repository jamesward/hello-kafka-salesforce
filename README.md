Hello Kafka Salesforce
----------------------

This simple app uses the Salesforce Streaming API to listen for events in Salesforce and then sends them to Kafka.

## Cloud Setup

1. [Signup for a Salesforce Developer Org](https://developer.salesforce.com/signup)
1. In Salesforce, create a PushTopic using the Execute Anonymous Apex feature in the Developer Console:

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

1. [Sign up for the Heroku Kafka preview](https://www.heroku.com/kafka)
1. [![Deploy on Heroku](https://www.herokucdn.com/deploy/button.svg)](https://heroku.com/deploy)
1. Add the Heroku Kafka Addon to the app

        heroku addons:add heroku-kafka - a YOUR_APP

1. Install the Kafka plugin into the Heroku CLI

        heroku plugins:install heroku-kafka

1. Wait for Kafka to be provisioned:

        heroku kafka:wait -a YOUR_APP

1. Add a new Kafka topic:

        heroku kafka:create ContactUpdates -a YOUR_APP

1. Watch the Kafka log

        heroku kafka:tail ContactUpdates -a YOUR_APP

1. Make a change to a Contact in Salesforce and you should see the event in the Kafka log.


## Local Setup

> This uses the same Kafka system as above.

1. Clone the source:

        git clone https://github.com/jamesward/hello-kafka-salesforce

1. Setup a `.env` file with the necessary info:

        heroku config -s > .env
        echo "SALESFORCE_USERNAME=<YOUR SALESFORCE USERNAME>" >> .env
        echo "SALESFORCE_PASSWORD=<YOUR SALESFORCE PASSWORD>" >> .env
        set -o allexport
        source .env
        set +o allexport

1. Setup the Kafka certs:

        bin/setup_certs.sh

1. Run the app:

        ./activator ~run
