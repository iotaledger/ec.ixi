package org.iota.ec;

import org.iota.ict.ixi.Ixi;
import org.iota.ict.ixi.IxiModule;
import org.iota.ict.ixi.context.IxiContext;
import org.iota.ict.ixi.context.SimpleIxiContext;
import org.json.JSONObject;

public class ECModule extends IxiModule {

    private final IxiContext context = new ECContext();

    public ECModule(Ixi ixi) {
        super(ixi);
    }

    @Override
    public void run() {
        System.err.println("EC is running!");
    }

    @Override
    public IxiContext getContext() {
        return context;
    }

    private Object performAction(String action, JSONObject requestJSON) {
        switch (action) {
            case "hello":
                return performActionHello(requestJSON);
            default:
                throw new IllegalArgumentException("unknown action '"+action+"'");
        }
    }

    private JSONObject performActionHello(JSONObject requestJSON) {
        return new JSONObject().put("hello", requestJSON.getString("name"));
    }

    private class ECContext extends SimpleIxiContext {

        public String respondToRequest(String request) {
            JSONObject requestJSON = new JSONObject(request);
            String action = requestJSON.getString("action");
            Object responseJSON = performAction(action, requestJSON);
            return responseJSON.toString();
        }
    }
}