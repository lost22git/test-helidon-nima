package lost.test.helidon;

public record Result<T>(
    T data,
    int code,
    String msg
) {

    public static <T> Result<T> ok(T data) {
        return new Result<>(data, 0, "");
    }

    public static Result<Void> err(int code,
                                   String msg) {
        return new Result<>(null, code, msg);
    }

    public static <T> Result<T> err2(int code,
                                     String msg) {
        return new Result<>(null, code, msg);
    }
}
