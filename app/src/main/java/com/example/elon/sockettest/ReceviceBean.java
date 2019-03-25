package com.example.elon.sockettest;

public class ReceviceBean {

    /**
     * code : 200
     * message : Success
     * data : {"CardID":"A90E4757"}
     */

    private String code;
    private String message;
    private DataBean data;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public static class DataBean {
        /**
         * CardID : A90E4757
         */

        private String CardID;

        public String getCardID() {
            return CardID;
        }

        public void setCardID(String CardID) {
            this.CardID = CardID;
        }
    }
}
