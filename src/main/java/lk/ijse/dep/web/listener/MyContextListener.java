package lk.ijse.dep.web.listener;

/**
 * @author : Dhanusha Perera
 * @since : 10/01/2021
 **/

import lk.ijse.dep.web.commonConstant.CommonConstant;
import org.apache.commons.dbcp2.BasicDataSource;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.HttpSessionBindingEvent;
import java.sql.SQLException;

@WebListener()
public class MyContextListener implements ServletContextListener{

    // Public constructor is required by servlet spec
    public MyContextListener() {
    }

    // -------------------------------------------------------
    // ServletContextListener implementation
    // -------------------------------------------------------
    public void contextInitialized(ServletContextEvent sce) {
      /* This method is called when the servlet context is
         initialized(when the Web application is deployed). 
         You can initialize servlet context related data here.
      */
//        System.out.println("Context is being initialized");

        BasicDataSource bds = new BasicDataSource();
        bds.setUsername(CommonConstant.MYSQL_USER_NAME);
        bds.setPassword(CommonConstant.MYSQL_PASSWORD);
        bds.setUrl(CommonConstant.MYSQL_URL);
        bds.setInitialSize(5);
        bds.setMaxTotal(5);
        ServletContext ctx = sce.getServletContext();
        ctx.setAttribute("cp",bds);
    }

    public void contextDestroyed(ServletContextEvent sce) {
      /* This method is invoked when the Servlet Context 
         (the Web application) is undeployed or 
         Application Server shuts down.
      */
//        System.out.println("Context is being destroyed");

        BasicDataSource cp = (BasicDataSource) sce.getServletContext().getAttribute("cp");

        try {
            cp.close();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

}
