# Contributing to Cryptora

Thank you for your interest in contributing to **Cryptora**! I strive to make the process of contributing as simple and clear as possible.

---

## How to Get Started?

1. **Familiarize Yourself with the Project**
    - Read the [README.md](./README.md) to understand the project's goals and features.
    - Ensure all required dependencies are installed as described in the "Installation Guide" section.

2. **Check Existing Issues**
    - Visit the [Issues section](https://github.com/dzenthai/cryptora/issues) of the project.
    - If you encounter a problem or have an idea for improvement, feel free to create a new issue.

---

## How to Report a Problem?

If you discover an issue, please follow these steps:

1. Ensure the issue hasn’t already been reported by checking existing issues.
2. Create a new issue and include the following details:
    - **Brief description of the problem**.
    - **Steps to reproduce** (so the issue can be replicated).
    - **Expected behavior** (what should happen instead).
    - **Environment details**:
        - Java Version:
        - Gradle Version:
        - Docker Version:
        - OS:
    - **Logs or screenshots** that help clarify the problem.

---

## How to Suggest a New Feature?

If you have an idea for a new feature:

1. Check if a similar feature request already exists.
2. Create a new issue and include:
    - **Feature description** (what you want to achieve).
    - **Why it’s important** (how it improves the project or solves a problem).
    - **Suggested implementation** (ideas for how it could be implemented).
    - **Additional context** (any extra information that could help).

---

## How to Make Changes?

1. **Fork the Repository**
   ```bash
   git fork https://github.com/dzenthai/cryptora.git
   cd cryptora
   ```
2. **Create a New Branch for Your Changes**
    ```bash
    git checkout -b feature/your-feature-name
    ```
3. **Make Changes to the Code**
   - Ensure your code follows the project's style.
   - Add tests if necessary.

4. **Test the Project Before Submitting**
   ```bash
   ./gradlew build
   ```
5. **Commit Your Changes**
   ```bash
   git commit -m "Add feature X to improve functionality Y"
   ```
6. **Push Your Changes**
   ```bash
   git push origin feature/your-feature-name
   ```
7. **Create a Pull Request**
   - Go to the original repository and create a pull request, detailing the changes you’ve made.