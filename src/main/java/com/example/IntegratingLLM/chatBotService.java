package com.example.IntegratingLLM;

import jakarta.annotation.PostConstruct;
//import org.apache.logging.log4j.message.Message;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class chatBotService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final List<Message> history=new ArrayList<>();

    @Value("classpath*:knowledge/*.pdf")
    private Resource[] policyFiles;

    public chatBotService(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
    }

    @PostConstruct
    public void loadKnowledgeBase(){
        //1:Read all pdf file
        //2:for each file divide them chunks
        //3:store those chunks in vector db ->> pinecone

        List<Document>allChnks=new ArrayList<>();

        TokenTextSplitter splitter=TokenTextSplitter.builder()
                .withChunkSize(300)
                .build();

        for(Resource resource :policyFiles){
            PagePdfDocumentReader reader= new PagePdfDocumentReader(resource);
            List<Document> pages=reader.read();
            List<Document> chunks=splitter.apply(pages);
            allChnks.addAll(chunks);
        }
        vectorStore.add(allChnks);
    }

    public String answerUserQuery(String question){
        //1:question ->vector
        //2:similarity search in vector db
        //3:top 4 result fetch


        List<Document> relevantChunks=vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(question)
                            .topK(4)
                            .build()
        );

        StringBuilder context=new StringBuilder();
        for(Document document:relevantChunks)
        {
            context.append(document.getText());
        }

        String finalContext=context.toString();
        String prompt=  """
                You are an AI customer support assistant for our e-commerce company.

                Answer the customer using ONLY the company information provided below.

                If the answer is not available in the provided information, say:
                "I don't have that information in the company documents."
                
                COMPANY INFORMATION
                %s
                
                """.formatted(finalContext);

        history.add(new UserMessage(question));

        String response= chatClient.prompt()
                .system(prompt)
                .messages(history)
                .call()
                .content();
        history.add((new AssistantMessage(response)));

        return response;
    }
}
