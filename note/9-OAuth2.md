## 1.概念

OAuth 2.0 是一个开放标准，该标准允许用户让第三方应用访问该用户在某一网站上存储的私密资源（如头像、照片、视频等），而在这个过程中无需将用户名和密码提供给第三方应用

在 OAuth 2.0 协议中定义了以下四个角色：

- **resource owner（资源拥有者）**：能够有权授予对保护资源访问权限的实体，也被称为最终用户。
- **resource server（资源服务器）**：承载受保护资源的服务器，能够接收使用访问令牌对受保护资源的请求并响应，它与授权服务器可以是同一服务器，也可以是不同服务器。
- **client（客户端）**：代表资源所有者及其授权发出对受保护资源请求的应用程序。
- **authorization server（授权服务器）**：认证服务器，即服务提供商专门用来处理认证授权的服务器。

OAuth2.0定义了四种授权模式，它们分别是：

- 授权码模式（authorization code）
- 简化模式（implicit）
- 密码模式（resource owner password credentials）
- 客户端模式（client credentials）

## 2. 模式详解

### 2.1 授权码模式

1. **用户**通过**客户端**点击授权登录按钮后，**客户端**会将请求通过URL重定向的方式跳转至**授权服务器**授权页面；
2. **用户**使用扫描二维码认证或者输入用户名密码后，**授权服务器**会向**资源服务器**验证用户身份信息的正确性
3. 如正确，则会生成一个临时凭证，并携带此凭证通过用户浏览器将请求重定向回**客户端**在第一次重定向时携带的callBackUrl地址；
4. 之后**用户**浏览器会携带临时凭证code访问**客户端**服务，**客户端**则通过此临时凭证再次调用**授权接口**，获取正式的访问凭据access_token；
5. 在**客户端**获取到授权访问凭据access_token后，此时用户的授权基本上就完成了，后续**客户端**要做的只是通过此token再访问**资源服务器**相关接口，获取允许授权开发的用户信息，如头像，昵称等，并据此完成自身的用户逻辑及用户登录会话逻辑。

### 2.2 简化模式

它的特点是不通过客户端服务器，而是直接在浏览器中向认证服务器申请令牌，跳过了“授权码临时凭证”这个步骤。其所有的步骤都在浏览器中完成，令牌对访问者是可见的，且客户端不需要认证

1. 和授权码模式1-2步骤一致
2. 认证服务器直接返回access_token令牌至用户浏览器端，省去了一个跳转步骤，提高了交互效率。
3. 这种方式访问令牌access_token会在URL片段中进行传输，因此可能会导致访问令牌被其他第三方截取，安全性上不是那么的高

### 2.3 密码模式

用户需要向客户端提供自己的用户名和密码，客户端使用这些信息向“服务提供商”索要授权（这种模式一般用在用户对客户端高度信任的情况下）

### 2.4 客户端模式

客户端模式是指客户端以自己的名义，而不是以用户的名义，向“服务提供方”进行认证。在这种模式下，用户并不需要对客户端授权，用户直接向客户端注册，客户端以自己的名义要求“服务提供商”提供服务。

## 3. 相关问题

### 3.1 为什么需要将授权码重定向给前端

假设从时序图中抹除授权码的流程，那么用户点击确定授权，此时资源拥有者与授权服务器就建立起关联，此时，资源拥有者则与第三方软件前端断开关联，界面则会停留在授权界面。然后授权服务器直接把access_token送给第三方软件后端，后端在携带access_token去访问受保护资源。虽然说资源数据已经拿到了，但是无法通知用户

### 3.2 为什么授权服务器不直接重定向传回access_token

重定向传回access_token，就在浏览器中暴露了，会增加access_token失窃的风险。在此层面上看，授权码的作用在于access_token不经过用户浏览器, 保护了access_token。

### 3.3 为什么授权码code可以暴露

1、授权码Authentication code只能用一次，而且会很快超时失效, 使得被截获后难以运用。

2、授权码需要和client id/client secret共同完成认证，才能够获得access_token。就算授权码如果失窃，单凭授权码是无法得到access_token的。

## 4. Spring Security

`spring security`使用目的：验证，授权，攻击防护。

原理：创建大量的filter和interceptor来进行请求的验证和拦截，以此来达到安全的效果。

### 4.1 Grant Types

- authorization_code — 授权码模式（即先登录获取code,再获取token）
- password — 密码模式（将用户名,密码传过去,直接获取token）
- client_credentials — 客户端模式（客户端用它自己的客户单凭证去请求获取访问令牌）
- implicit — 简化模式(在redirect_uri 的 Hash 传递token; Auth客户端运行在浏览器中,如JS,Flash)
- refresh_token — 刷新access_token

### 4.2 oauth2内置接口清单

- /oauth/authorize：授权端点

- /oauth/token：获取令牌端点

- /oauth/confirm_access：用户确认授权提交端点

- /oauth/error：授权服务错误信息端点

- /oauth/check_token：用于资源服务访问的令牌解析端点

- /oauth/token_key：提供公有密匙的端点，如果你使用JWT令牌的话

### 4.3 受保护的资源配置

受保护的资源（或者叫远程资源）可以用OAuth2ProtectedResourceDetails类型的bean来定义。一个被保护的资源由下列属性：

- id ：资源的id。这个id只是用于客户端查找资源。
- clientId ：OAuth Client id。
- clientSecret ：关联的资源的secret。默认非空
- accessTokenUri ：提供access_token的端点的uri
- scope ：逗号分隔的字符串，代表访问资源的范围。默认为空
- clientAuthenticationScheme ：客户端认证所采用的schema。建议的值："http_basic"和"form"。默认是"http_basic"。

不同的授权类型有不同的OAuth2ProtectedResourceDetails的具体实现（例如：ClientCredentialsResourceDetails是"client_credentials"类型的具体实现）

- userAuthorizationUri ：用户授权uri，非必需的。

### 4.4 AuthorizationServerConfigurerAdapter

配置OAUth2 授权服务器的配置类接口，添加了@EnableAuthorizationServer，spring会自动注入。接口有三个方法，可以实现客户端配置、安全功能、以及各个Endpoint（端点）的相关配置。

```java
@Configuration
// 开启Oauth2认证
@EnableAuthorizationServer
public class AuthorizationServer extends AuthorizationServerConfigurerAdapter {
    
    //用来配置令牌端点(Token Endpoint)的安全约束
    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception { 
        security
                // 开启/oauth/token_key验证端口无权限访问
                .checkTokenAccess("permitAll()")
                //主要是让/oauth/token支持client_id和client_secret做登陆认证如果开启了,
                //那么就在BasicAuthenticationFilter之前
                //添加ClientCredentialsTokenEndpointFilter,使用ClientDetailsUserDetailsService来进行登陆认证
                .allowFormAuthenticationForClients();
    }
    //用来配置客户端详情服务（ClientDetailsService），客户端详情信息在这里进行初始化
    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
         // 自定义
        clients.withClientDetails(clientService);
    }
    //用来配置授权（authorization）以及令牌（token）的访问端点和令牌服务(token services)。
    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints
                // .authenticationManager(authenticationManager) // 密码模式
                .tokenServices(tokenServices()); // 设置检验token 服务配置
    }
    //密码的加密方式
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }    
```

### 4.5 ResourceServerConfigurerAdapter

```java
@Slf4j
@EnableResourceServer
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
@ConditionalOnProperty(prefix = "paas.auth", value = "resource-enabled", havingValue = "true")
@Order(1)
public class ResourceAutoConfiguration extends ResourceServerConfigurerAdapter {

    @Bean
    PaasResourceServerTokenServices tokenServices() {
    }    
    
    /**
     * ResourceServerSecurityConfigurer主要配置以下几方面：
     * tokenServices：ResourceServerTokenServices 类的实例，用来实现令牌访问服务，如果资源服务和授权服务不在一块，就需要 通过RemoteTokenServices来访问令牌
     * tokenStore：TokenStore类的实例，定义令牌的访问方式
     * resourceId：这个资源服务的ID
     * 其他的拓展属性例如 tokenExtractor 令牌提取器用来提取请求中的令牌。
     */
    @Override
    public void configure(ResourceServerSecurityConfigurer resources) {
        // 设置资源id
        resources.resourceId(authProperties.getResourceId()).tokenServices(tokenServices());
    }
    
        @Override
    public void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                // 配置文件中的 URL 进行拦截
                .antMatchers(authProperties.getResourceUrls()).authenticated()
                // 放行其他 URL
                .anyRequest().permitAll();
    }
```

