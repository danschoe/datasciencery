#from .init_project import *
from apps import app


if __name__ == "__main__":
    app.run(host='0.0.0.0')
