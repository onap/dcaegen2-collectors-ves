## VES documentation
###Requirements
    Python 3.x

To generate a documentation locally follow below steps.

1. Open **docs** folder in terminal
2. Install all required dependencies

    ```
    pip install -r requirements-docs.txt
   ```
3. Generate local documentation
    ```
    make html
    ```
    After command execution the documentation will be available in **_build/html** folder.
4. Before you commit documentation changes please execute
    ```
    make clean
   ```
