package jackrabbitdemo;

import java.io.*;
import java.security.NoSuchAlgorithmException;

import javax.jcr.*;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.security.authentication.CryptedSimpleCredentials;
import org.apache.jackrabbit.rmi.repository.URLRemoteRepository;

public class Main implements EventListener {


    private static Session session;

	public static void main(String[] args) throws LoginException, RepositoryException, NoSuchAlgorithmException, IOException, InterruptedException {
		Credentials defautlCredentials = new SimpleCredentials("admin", "admin".toCharArray());
//		Repository repository = new TransientRepository("/home/satyadeep/jackrabbit/jackrabbit/repository.xml",
//				"/home/satyadeep/jackrabbit/jackrabbit");
        Repository repository;
        repository = new URLRemoteRepository("http://localhost:8082/rmi");
        session = repository.login(defautlCredentials);
        ObservationManager obsManager = session.getWorkspace().getObservationManager();


        String[] nodeTypes = new String[]{"nt:unstructured"};
        obsManager.addEventListener(new Main(), Event.NODE_ADDED|Event.NODE_MOVED| Event.NODE_REMOVED, "/content/contacts/hr", true, null, nodeTypes, false);

        removeContent();
        createContent();

        Thread.sleep(30000);
        session.logout();
	}

    private static void removeContent() throws RepositoryException {
        Node content;
        try {
            content = session.getRootNode().getNode("content");
            content.remove();
        }
        catch (PathNotFoundException e) {
            // do nothing, the node does not exist
        }
    }

    private static void createContent() throws RepositoryException, IOException {
        // prepare the workspace with initial content
        session.importXML("/", new FileInputStream(new File("src/main/resources/content.xml")), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        session.save();
    }

    @Override
    public void onEvent(EventIterator events) {
        try {
            while (events.hasNext()) {
                Event event = events.nextEvent();
                if (event.getType() == Event.NODE_ADDED) {
                    Node node = session.getNode(event.getPath());
                    System.out.println("Node added:" + node);
                    session.save();
                } else if (event.getType() == Event.NODE_REMOVED) {
                    Node node = session.getNode(event.getPath());
                    PropertyIterator itr = node.getProperties();
                    while(itr.hasNext()){
                        Property property = (Property)itr.next();
                        System.out.println(property.getString());
                    }
                    System.out.println("Node removed:" + node);
                }
            }
        }
        catch (RepositoryException e) {
            e.printStackTrace();
        }
    }
}
