package entropy.plan.action;

import entropy.configuration.Configuration;
import entropy.configuration.Node;


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
		boolean b = this.getNode().setCurrentPlatform(newPlatform);
		c.addOnline(this.getNode()); // never fail
		return b;
	}
}
