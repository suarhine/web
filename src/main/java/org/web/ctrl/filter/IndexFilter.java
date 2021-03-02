/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.web.ctrl.filter;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.Globals;
import org.apache.catalina.WebResourceRoot;
import org.apache.tomcat.util.http.fileupload.FileUploadException;

/**
 *
 * @author เสือไฮ่
 */
public class IndexFilter implements Filter {

    private class Auth {

        private final String[] path;
        private final String[] method;
        private final String[] role;
        private final char flag;

        public Auth(String[] path, String[] method, String[] role, char flag) {
            this.path = path;
            this.method = method;
            this.role = role;
            this.flag = flag;
        }
    }
    private List<Auth> auth;
    private FilterConfig config;
    private boolean listings;

    @Override
    public void init(FilterConfig config) throws ServletException {
        Filter.super.init(this.config = config);
        this.listings = "TRUE".equalsIgnoreCase(
                config.getInitParameter("listings")
        );
        try {
            this.auth = new ArrayList<>();
            for (var entry : config.getInitParameter("auth").split(
                    " *\r?\n *"
            )) {
                if (entry.isBlank()) {
                    continue;
                }
                var value = entry.split(" *: *");
                var path = value[0].split(" *, *");
                try {
                    var method = value[1].isEmpty() || value[1].equals("*")
                            ? null : value[1].split(" *: *", 2);
                    try {
                        switch (value[2]) {
                        case "" ->
                            auth.add(new Auth(path, method, null, ' '));
                        case "*" ->
                            auth.add(new Auth(path, method, null, '*'));
                        case "+" ->
                            auth.add(new Auth(path, method, null, '+'));
                        case "-" ->
                            auth.add(new Auth(path, method, null, '-'));
                        case "#" ->
                            auth.add(new Auth(path, method, null, '#'));
                        default -> {
                            char flag = 0;
                            var matcher = Pattern.compile(
                                    "(^| *,) *(\\+) *(, *|$)"
                            ).matcher(value[2]);
                            if (matcher.find()) {
                                flag = '*';
                                value[2] = matcher.replaceAll(
                                        r -> r.group().matches(
                                                " *, *(\\+) *, *"
                                        ) ? ", " : ""
                                );
                            }
                            var role = value[2].isEmpty()
                                    ? null : value[2].split(" *, *");
                            auth.add(new Auth(path, method, role, flag));
                        }
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        auth.add(new Auth(path, method, null, ' '));
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    auth.add(new Auth(path, null, null, ' '));
                }
            }
        } catch (NullPointerException e) {
        }
    }

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {
        try {
            doFilter(
                    (HttpServletRequest) request,
                    (HttpServletResponse) response,
                    chain
            );
        } catch (ClassCastException e) {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        this.auth = null;
        this.listings = false;
        this.config = null;
        Filter.super.destroy();
    }

    public WebResourceRoot getWebResourceRoot() {
        return (WebResourceRoot) config.getServletContext()
                .getAttribute(Globals.RESOURCES_ATTR);
    }

    private List<Map.Entry<Integer, Auth>> getAuthList(
            String path, String method
    ) {
        var list = new ArrayList<Map.Entry<Integer, Auth>>();
        for (var auth : auth) {
            for (var split : auth.path) {
                if (split.endsWith("/")
                        ? path.startsWith(split)
                        : path.replaceAll("/$", "").equals(split)) {
                    if (auth.method == null) {
                        list.add(Map.entry(split.length(), auth));
                        continue;
                    }
                    for (var value : auth.method) {
                        if (method.equalsIgnoreCase(value)) {
                            list.add(Map.entry(split.length(), auth));
                            break;
                        }
                    }
                }
            }
        }
        Collections.sort(list, (o1, o2) -> o2.getKey() - o1.getKey());
        return list;
    }

    public void doFilter(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {
        var path = request.getRequestURI()
                .substring(request.getContextPath().length());
main:   for (var entry : getAuthList(path, request.getMethod())) {
            var auth = entry.getValue();
            switch (auth.flag) {
            case ' ' -> {
                break main;
            }
            case '-' -> {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            case '#' -> {
                chain.doFilter(request, response);
                return;
            }
            }
            if (request.authenticate(response)) {
                if (auth.flag == '*') {
                    break;
                }
                try {
                    for (var role : auth.role) {
                        if (request.isUserInRole(role)) {
                            break main;
                        }
                    }
                } catch (NullPointerException e) {
                }
                if (auth.flag != '+') {
                    response.setHeader(
                            "WWW-Authenticate",
                            "BASIC realm=\"Tomcat Manager Application\""
                    );
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            } else {
                return;
            }
        }
        if (listings && path.endsWith("/")) {
            try {
                if ("POST".equals(request.getMethod())) {
                    upload(request, response, path);
                } else {
                    request.setAttribute(
                            "entries", getWebResourceRoot().listResources(path)
                    );
                    request.setAttribute("path", path);
                    request.getRequestDispatcher("/WEB-INF/jsp/")
                            .forward(request, response);
                }
            } catch (Throwable e) {
                throw e instanceof ServletException
                        ? (ServletException) e : new ServletException(e);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    public void upload(
            HttpServletRequest request,
            HttpServletResponse response,
            String path
    ) throws IOException, ServletException, URISyntaxException {
        var dir = new File(
                getWebResourceRoot().getResource(path).getCanonicalPath()
        );
        try {
            if (!dir.exists()) {
                dir.mkdirs();
            }
            for (var part : request.getParts()) {
                if (part.getName().equals("file")) {
                    Files.copy(
                            part.getInputStream(),
                            new File(dir, part.getSubmittedFileName()).toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }
            }
            response.sendRedirect(".");
        } catch (ServletException e) {
            if (!(e.getRootCause() instanceof FileUploadException)) {
                throw e;
            }
            if (request.getParameter("delete") != null) {
                try {
                    for (var name : request.getParameterValues("name")) {
                        if (!new File(dir, name).delete()) {
                            throw new ServletException(path + name);
                        }
                    }
                } catch (NullPointerException e2) {
                }
                response.sendRedirect(".");
            } else if (request.getParameter("delete-dir") != null) {
                if (new Function<File, Boolean>() {
                    @Override
                    public Boolean apply(File file) {
                        if (file.isDirectory()) {
                            for (var child : file.listFiles()) {
                                if (!apply(child)) {
                                    return false;
                                }
                            }
                        }
                        return file.delete();
                    }
                }.apply(dir)) {
                    response.sendRedirect("..");
                } else {
                    throw new ServletException(path);
                }
            } else {
                var subdir = request.getParameter("dir");
                if (subdir != null && !subdir.isBlank()) {
                    var child = new File(dir, subdir);
                    if (child.mkdir() || child.isDirectory()) {
                        response.sendRedirect(child.getName());
                    } else {
                        throw new ServletException(path + subdir);
                    }
                }
            }
        }
    }
}
