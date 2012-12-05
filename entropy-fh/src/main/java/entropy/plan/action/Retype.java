package entropy.plan.action;

import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.SimpleNode;


public class Retype extends Startup {
    private String newPlatform;

    public Retype(Node n, int s, int f, String newPlatform) {
        super(n, s, f);
        this.newPlatform = newPlatform;
    }

    public Retype(Node n, String newPlatform) {
    	this(n, 0, 0, newPlatform);
    }

    @Override
	public boolean apply(Configuration c) {
        Node newNode = this.getNode().clone();
        ManagedElementSet<Node> nodes = c.getOnlines();
        nodes.remove(this.getNode());


		boolean b = newNode.setCurrentPlatform(newPlatform);
		c.addOnline(newNode); // never fail
		return b;
	}

    @Override
    public boolean isCompatibleWith(Configuration src) {
        return true;
    }
}
