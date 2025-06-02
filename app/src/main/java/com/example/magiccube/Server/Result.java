package com.example.magiccube.Server;
import androidx.annotation.NonNull;

public class Result {

    private static final String SUCCESS_CODE = "200";
    private static final String ERROR_CODE = "-1";

    private String code;
    private Object data;
    private String msg;

    public String getCode(){
        return this.code;
    }

    public Object getData(){
        return this.data;
    }

    public String getMsg(){
        return this.msg;
    }

    private void setCode(String code){
        this.code=code;
    }
    private void setData(Object data){
        this.data=data;
    }

    private void setMsg(String msg){
        this.msg=msg;
    }


    public static Result success() {
        Result result = new Result();
        result.setCode(SUCCESS_CODE);
        return result;
    }

    public static Result success(Object data) {
        Result result = new Result();
        result.setCode(SUCCESS_CODE);
        result.setData(data);
        return result;
    }

    public static Result error(String code, String msg) {
        Result result = new Result();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }


    @NonNull
    @Override
    public String toString() {
        return "Result{" +
                "code='" + code + '\'' +
                ", data=" + data +
                ", msg='" + msg + '\'' +
                '}';
    }
}
