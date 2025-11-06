**It is already the year 2025.**

### **Part 1: Core Testing Directive**

*   **1. Test Execution:** All testing tasks **MUST** be performed by the user.
*   **2. Your Responsibility:** During the testing phase, your sole responsibility is to **provide the user with clear, complete, and actionable testing steps**. You must explicitly state how the user should operate, what to check, and what the expected results are. It is **STRICTLY PROHIBITED** to attempt to perform the tests yourself.
* *  **3. Your Language:** You MUST use type's language like Typescript Not to use Javascript.

### **Part 2: Core Interaction Principles**

This section is the highest priority and must be strictly followed at all times.

*   **0. Communication Language:** All communication with the user **MUST** be in Chinese.

*   **1. Task Plan Confirmation**
    *   After creating any task list or plan, you **MUST** obtain confirmation from the user regarding the correctness and completeness of the tasks via `mcp-feedback-enhanced`.
    *   You may only begin the specific implementation steps after the user has confirmed that the task list is correct.

### **Part 3: Task Handling & Execution Flow**

*   **1. Requirement Clarification:** Before starting any task, you **MUST** first analyze the requirements. If the requirements are vague, ambiguous, or missing key information, you **MUST** proactively ask questions to clarify. It is **STRICTLY PROHIBITED** to make assumptions or guesses when requirements are unclear.

*   **2. Formulate an Execution Plan:** After understanding the requirements, you **MUST** break down the task into a series of specific, executable steps and present it to the user as a task list.

*   **3. Incremental Development:** For complex features, adopt an incremental, step-by-step development approach. After completing a small, verifiable step, perform a build check or test and report the progress to the user, rather than writing all the code at once.

*   **4. Solution Decision-Making:** When multiple solutions exist for a problem, you **MUST** first explain in detail the pros and cons of each solution, and then execute the one chosen by the user based on their instructions. It is **STRICTLY PROHIBITED** to proceed with a solution without prior communication.

### **Part 4: Coding & Engineering Standards**

**4.1. API Request Standards**

*   **Use the Centralized Request Utility:** It is **STRICTLY PROHIBITED** to use native `fetch()` or `axios()` directly in the code. All backend API requests **MUST** be made through the project's encapsulated, centralized request module (e.g., located at `src/utils/http.js`).

*   **Prohibit Hard-Coding API Addresses:** It is **STRICTLY PROHIBITED** to have `localhost` or any hard-coded IP/domain in the code. The Base URL for all requests **MUST** be read from environment variables or configuration files.

*   **Unified Error Handling:** The request utility **MUST** include unified interceptors for centrally managing request headers, global loading states, and HTTP errors.

*   **4.1.5 Unified Data Format:** Front-end and back-end must use a unified data format for API responses. The back-end should use a dedicated utility library to ensure consistent response structure. The front-end should use a unified approach to destructure and process response data through the request utility. This includes handling success, failure, and error scenarios consistently.

**4.2. Code Style & Formatting**

*   **Adhere to Project Style Guide:** Strictly follow the rules configured in the project's linter tools, such as ESLint and Prettier.

*   **4.2.1 Standardized Naming Conventions:** All response attributes from the backend should adhere to a standard naming convention. For example, using snake_case for attribute names in JSON responses.

*   **Naming Conventions:**
    *   Variables/Functions: Use **camelCase**.
    *   Constants: Defined in constant files, use **UPPER_SNAKE_CASE**.
    *   Classes/Components: Use **PascalCase**.
    *   Names **MUST** be semantically clear. Vague or overly short names are **STRICTLY PROHIBITED**.

*   **Directory Structure:** You **MUST** organize the directory structure according to functional modules.

**4.3. Modularity & Reusability**

*   **Encapsulate Common Logic:** Reusable logic (e.g., date formatting, data validation) **MUST** be encapsulated into separate utility functions and placed in a unified `utils` directory.

*   **Single Responsibility Principle:** Each function, component, or module should focus on a single core responsibility.

**4.4. State Management**

*   **Follow Established Patterns:** If the project uses a state management library (e.g., Pinia, Redux), all cross-component or global shared state **MUST** be managed through that library.

**4.5. Constants Management**

*   **Avoid Magic Values:** It is **STRICTLY PROHIBITED** to use unexplained strings or numbers directly in the code. All such constants **MUST** be defined in a dedicated constants file.

**4.6. Dependency Management**

*   **Prefer Existing Dependencies:** Before adding a new library, you **MUST** check if an existing library in the project can achieve the same functionality.

*   **Request Approval for New Dependencies:** If a new dependency is necessary, you **MUST** explain the reason to the user and obtain their approval.

**4.7. Documentation & Comments**

*   **Comment Complex Logic:** For complex business rules or non-intuitive code, you **MUST** add comments to explain its purpose and how it works.

*   **JSDoc/TSDoc:** Add standard documentation comments for all public functions (especially utility functions).

### **Part 5: Code Quality & Security Assurance**

*   **1. Build Check:** After every code change, you **MUST** use the corresponding language's `Build` command to check for any compile-time errors.

*   **2. Testing First:**
    *   Write unit tests for new business logic (especially `utils` functions), covering normal, boundary, and exceptional cases.
    *   You **MUST** use the project's existing testing framework.

*   **3. Error Handling:**
    *   You **MUST** add `try...catch` blocks or equivalent error handling for operations that can fail (e.g., API requests, data parsing).
    *   It is **STRICTLY PROHIBITED** to silently ignore errors. Provide clear feedback to the user or log the error.

*   **4. Security:**
    *   **Prohibit Hard-Coding Sensitive Information:** API keys, passwords, etc., **MUST** be managed via environment variables.
    *   **Prevent XSS:** When rendering user-provided or API-sourced data, you **MUST** ensure it is properly sanitized or escaped.

### **Part 6: Specific Tech & Tooling Rules**

*   **1. Project Structure:** Always follow the simplest possible implementation structure unless the user explicitly requests a special requirement.

*   **2. ArcGIS Object Handling:**
    *   It is **STRICTLY PROHIBITED** to use `ref` (or similar reactive references) to store ArcGIS objects, as this will cause errors when proxy objects access read-only properties.
    *   **Correct Approach:** Use a plain variable (`let map = null`) to store the ArcGIS object instance. For data that requires reactive updates, convert the object's contents into plain JavaScript objects before storing them in a reactive state.

*   **3. Tool Usage:**
    *   **Search:** Prioritize using `tavily` for searches.
    *   **API Documentation:** When unsure about an API, use the `Context7` tool to find relevant documentation.
    *   **API Testing:** Prioritize using the `CURL` command for API testing to improve efficiency.
    *   **Frontend Environment:** Use `bun` and `bunx` to install dependencies and run scripts.
    *   **Container Environment:** Use `podman` for container management.
    *   **Server Startup:** It is **PROHIBITED** to perform the action of starting the development server. The user will handle this.