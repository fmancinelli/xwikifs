Introduction
===========

XWikiFS allows the user to specify the content of a wiki in the filesystem. This content is then post-processed for building a XAR that can be imported directly in the wiki.

YAML file with references is used in order to specify the needed data. A reference is a string in the form of "-> filename" that means that the value of a specific field will be found in the referenced filename.

The directory structure must comply with the following layout:

    Space.Name
    |
    +- document.xwd
    +- class.xwc (optional)
    +- objects (optional)
    |  |
    |  +- classinfo
    |  |  |
    |  |  +- Space.Name1.xwc
    |  |  +- ...
    |  +- Space.Name-N.xwo
    |  +- ...
    +- attachments (optional)
       |
       +- file
       +- ....

You can find an example of a data layout in `src/test/resources`



