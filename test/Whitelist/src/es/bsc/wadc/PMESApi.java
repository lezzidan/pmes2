package es.bsc.wadc;


public class PMESApi {
    public static String test(){
        return "HOLA MUNDO";
    }

    public static Boolean authorizeUser(String userName){
        if (userName.equals("scorellap@gmail.com")){
            return Boolean.TRUE;
        } else{
            return Boolean.FALSE;
        }
    }

    public static void main(String[] args) {
	// write your code here
        System.out.println(test());
    }
}
