package cn.pylin.xycjd.service;


import android.os.RemoteException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import cn.pylin.xycjd.IUserShellService;

public class UserShellService extends IUserShellService.Stub {

    @Override
    public void destroy() throws RemoteException {
        System.exit(0);
    }

    @Override
    public String execLine(String command) throws RemoteException {
        try {
            Process p = Runtime.getRuntime().exec(command);
            return read(p);
        } catch (Exception e) {
            throw new RemoteException(e.getMessage());
        }
    }

    private String read(Process p) throws Exception {
        BufferedReader r = new BufferedReader(
                new InputStreamReader(p.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            sb.append(line).append('\n');
        }
        p.waitFor();
        return sb.toString();
    }
}