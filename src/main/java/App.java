/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/**
 * This program will demonstrate the file transfer from remote to local
 *   $ CLASSPATH=.:../build javac ScpFrom.java
 *   $ CLASSPATH=.:../build java ScpFrom user@remotehost:file1 file2
 * You will be asked passwd.
 * If everything works fine, a file 'file1' on 'remotehost' will copied to
 * local 'file1'.
 *
 */
import com.jcraft.jsch.*;
import java.io.*;

public class App {
  public static void main(String[] arg) {
    if(arg.length != 2) {
      System.err.println("usage: java ScpFrom user@remotehost:file1 file2");
      System.exit(-1);
    }

    FileOutputStream fos = null;

    try {
      String user = arg[0].substring(0, arg[0].indexOf('@'));
      arg[0] = arg[0].substring(arg[0].indexOf('@') + 1);
      String host = arg[0].substring(0, arg[0].indexOf(':'));
      String rfile = arg[0].substring(arg[0].indexOf(':') + 1);
      String lfile = arg[1];

      String prefix = null;

      if(new File(lfile).isDirectory()) {
        prefix = lfile + File.separator;
      }

      JSch jsch = new JSch();
      Session session = jsch.getSession(user, host, 22);

      UserInfo ui = new MyUserInfo();
      session.setUserInfo(ui);
      session.connect();

      String command = "scp -f " + rfile;
      Channel channel = session.openChannel("exec");
      ((ChannelExec)channel).setCommand(command);

      OutputStream out = channel.getOutputStream();
      InputStream in = channel.getInputStream();
      channel.connect();

      byte[] buf = new byte[1024];
      buf[0] = 0;
      out.write(buf, 0, 1);
      out.flush();

      while(true) {
	      int c = checkAck(in);

        if(c != 'C') {
	        break;
	      }

        in.read(buf, 0, 5);

        long filesize = 0L;

        while(true) {
          if(in.read(buf, 0, 1) < 0) {
            // error
            break;
          }

          if(buf[0]==' ') break;
          filesize = filesize * 10L + (long)(buf[0] - '0');
        }

        String file;
        for(int i=0;;i++) {
          in.read(buf, i, 1);
          if(buf[i] == (byte)0x0a) {
            file = new String(buf, 0, i);
            break;
  	      }
        }

        // send '\0'
        buf[0]=0;
        out.write(buf, 0, 1);
        out.flush();

        // read a content of lfile
        fos = new FileOutputStream(prefix == null ? lfile : prefix + file);
        int foo;
        while(true) {
          if(buf.length < filesize) foo = buf.length;
	        else foo = (int)filesize;
          foo = in.read(buf, 0, foo);
          if(foo < 0) {
            // error
            break;
          }
          fos.write(buf, 0, foo);
          filesize -= foo;
          if(filesize == 0L) break;
        }
        fos.close();
        fos=null;

        if(checkAck(in) != 0) {
          System.exit(0);
        }

        // send '\0'
        buf[0]=0;
        out.write(buf, 0, 1);
        out.flush();
      }

      session.disconnect();

      System.exit(0);
    }
    catch(Exception e) {
      System.out.println(e);
      try {
        if(fos != null)
          fos.close();
      }
      catch(Exception ignore) {}
    }
  }

  static int checkAck(InputStream in) throws IOException {
    int b = in.read();
    if(b == 0) return b;
    if(b == -1) return b;

    if(b==1 || b==2) {
      StringBuffer sb = new StringBuffer();
      int c;

      do {
	      c = in.read();
	      sb.append((char)c);
      } while(c != '\n');

      if(b == 1) {
	      System.out.print(sb.toString());
      }
      if(b == 2) {
	      System.out.print(sb.toString());
      }
    }

    return b;
  }
}
