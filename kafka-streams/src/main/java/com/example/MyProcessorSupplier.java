package com.example;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.messaging.Message;

import java.util.Locale;

/**
 * Created by amarendra on 16/3/17.
 */
public class MyProcessorSupplier implements ProcessorSupplier<String, Message<String>> {

    @Override
    public Processor<String, Message<String>> get() {
        return new Processor<String, Message<String>>() {
            private ProcessorContext context;
            private KeyValueStore<String, Integer> kvStore;

            @Override
            @SuppressWarnings("unchecked")
            public void init(ProcessorContext context) {
                this.context = context;
                this.context.schedule(1000);
                this.kvStore = (KeyValueStore<String, Integer>) context.getStateStore("Counts");
            }

            @Override
            public void process(String dummy, Message<String> line) {
                String[] words = line.getPayload().toLowerCase(Locale.getDefault()).split(" ");

                for (String word : words) {
                    Integer oldValue = this.kvStore.get(word);

                    if (oldValue == null) {
                        this.kvStore.put(word, 1);
                    } else {
                        this.kvStore.put(word, oldValue + 1);
                    }
                }

                context.commit();
            }

            @Override
            public void punctuate(long timestamp) {
                try (KeyValueIterator<String, Integer> iter = this.kvStore.all()) {
                    System.out.println("----------- " + timestamp + " ----------- ");

                    while (iter.hasNext()) {
                        KeyValue<String, Integer> entry = iter.next();

                        System.out.println("[" + entry.key + ", " + entry.value + "]");

                        context.forward(entry.key, entry.value.toString());
                    }
                }
            }

            @Override
            public void close() {
                this.kvStore.close();
            }
        };
    }
}
