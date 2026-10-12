# Dependencies and source references

Project basis: the supplied `DOC-20260914-WA0191.pdf`, page 1; additional feature requirements in the user's chat. Implementation decisions are documented separately in REQUIREMENTS.md.

Official technical references checked while preparing the source (12 October 2026; hosting interfaces and versions can change):

- Spring Boot 4.0.8 release: https://spring.io/blog/2026/08/20/spring-boot-4-0-8-available-now/
- Java/build compatibility: https://docs.spring.io/spring-boot/4.0/system-requirements.html
- Boot 4 starters: https://docs.spring.io/spring-boot/4.0/reference/using/build-systems.html
- Jackson 3 integration: https://docs.spring.io/spring-boot/4.0/reference/features/json.html
- MVC test configuration: https://docs.spring.io/spring-boot/4.0/api/java/org/springframework/boot/webmvc/test/autoconfigure/AutoConfigureMockMvc.html
- Spring Security CSRF: https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
- React versions: https://react.dev/versions
- Vite requirements/building: https://vite.dev/guide/
- Railway Spring Boot deployment: https://docs.railway.com/guides/spring-boot
- Railway MySQL: https://docs.railway.com/databases/mysql
- Railway persistent storage: https://docs.railway.com/guides/volumes
- Railway health checks: https://docs.railway.com/deployments/healthchecks
- Railway legacy config deprecation: https://docs.railway.com/config-as-code
- Railway plans: https://docs.railway.com/pricing/plans
- Vercel rewrites: https://vercel.com/docs/rewrites
- Vercel Vite deployment: https://vercel.com/docs/frameworks/frontend/vite
- Playwright cloud CI: https://playwright.dev/docs/ci

The application uses no paid application API, external font, analytics tracker, banking API or LLM API. Hosting itself can incur charges. Third-party packages are downloaded by the online build and retain their own licenses; no third-party binaries or fonts are included.

Maven's parent manages compatible backend dependency versions. The npm package files define compatible version ranges. A complete dependency resolution/security audit was not possible in the creation environment. Run the online workflow, inspect dependency advisories and commit generated npm lockfiles before wider deployment. These reference checks do not substitute for compiling and running the application.
