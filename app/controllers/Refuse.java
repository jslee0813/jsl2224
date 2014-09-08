package controllers;

import play.mvc.*;

import models.*;

public class Refuse extends Controller {

    public static Result refuse(String borough, int commId) throws Exception {
        return ok(RefuseData.getRefuseAmount(borough, commId));
    }

    public static Result paper(String borough, int commId) throws Exception {
        return ok(RefuseData.getPaperAmount(borough, commId));
    }
    
    public static Result mgp(String borough, int commId) throws Exception {
        return ok(RefuseData.getMGPAmount(borough, commId));
    }
    
    public static Result total() throws Exception {    	
        return ok(RefuseData.getTotal());
    }
    
    public static Result error(String badURL) {    	
        return ok("0");
    }
}

