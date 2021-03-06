package com.github.zhangkaitao.shiro.chapter12;

import com.github.zhangkaitao.shiro.chapter12.entity.Permission;
import com.github.zhangkaitao.shiro.chapter12.entity.Role;
import com.github.zhangkaitao.shiro.chapter12.entity.User;
import com.github.zhangkaitao.shiro.chapter12.realm.UserRealm;
import com.github.zhangkaitao.shiro.chapter12.service.PermissionService;
import com.github.zhangkaitao.shiro.chapter12.service.RoleService;
import com.github.zhangkaitao.shiro.chapter12.service.UserService;
import junit.framework.Assert;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;

import javax.sql.DataSource;

/**
 * <p>User: Zhang Kaitao
 * <p>Date: 14-2-12
 * <p>Version: 1.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:spring-beans.xml", "classpath:spring-shiro.xml"})
@TransactionConfiguration(defaultRollback = false)
public class ShiroTest {

    @Autowired
    protected PermissionService permissionService;
    @Autowired
    protected RoleService roleService;
    @Autowired
    protected UserService userService;

    @Autowired
    private UserRealm userRealm;

    
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    private void setDataSource(DataSource ds) {
        jdbcTemplate = new JdbcTemplate(ds);    
    }


    protected String password = "123";

    protected Permission p1;
    protected Permission p2;
    protected Permission p3;
    protected Role r1;
    protected Role r2;
    protected User u1;
    protected User u2;
    protected User u3;
    protected User u4;
    
    @Before
    public void setUp() {
        jdbcTemplate.update("delete from sys_users");
        jdbcTemplate.update("delete from sys_roles");
        jdbcTemplate.update("delete from sys_permissions");
        jdbcTemplate.update("delete from sys_users_roles");
        jdbcTemplate.update("delete from sys_roles_permissions");


        //1???????????????
        p1 = new Permission("user:create", "??????????????????", Boolean.TRUE);
        p2 = new Permission("user:update", "??????????????????", Boolean.TRUE);
        p3 = new Permission("menu:create", "??????????????????", Boolean.TRUE);
        permissionService.createPermission(p1);
        permissionService.createPermission(p2);
        permissionService.createPermission(p3);
        //2???????????????
        r1 = new Role("admin", "?????????", Boolean.TRUE);
        r2 = new Role("user", "???????????????", Boolean.TRUE);
        roleService.createRole(r1);
        roleService.createRole(r2);
        //3???????????????-??????
        roleService.correlationPermissions(r1.getId(), p1.getId());
        roleService.correlationPermissions(r1.getId(), p2.getId());
        roleService.correlationPermissions(r1.getId(), p3.getId());

        roleService.correlationPermissions(r2.getId(), p1.getId());
        roleService.correlationPermissions(r2.getId(), p2.getId());

        //4???????????????
        u1 = new User("zhang", password);
        u2 = new User("li", password);
        u3 = new User("wu", password);
        u4 = new User("wang", password);
        u4.setLocked(Boolean.TRUE);
        userService.createUser(u1);
        userService.createUser(u2);
        userService.createUser(u3);
        userService.createUser(u4);
        //5???????????????-??????
        userService.correlationRoles(u1.getId(), r1.getId());

    }

    @Test
    public void test() {
        Subject subject = SecurityUtils.getSubject();
        UsernamePasswordToken token = new UsernamePasswordToken(u1.getUsername(), password);
        subject.login(token);

        Assert.assertTrue(subject.isAuthenticated());
        subject.checkRole("admin");
        subject.checkPermission("user:create");

        userService.changePassword(u1.getId(), password + "1");
        userRealm.clearCache(subject.getPrincipals());

        token = new UsernamePasswordToken(u1.getUsername(), password + "1");
        subject.login(token);




    }

}
