
# DSpace

[![Build Status](https://travis-ci.com/DSpace/DSpace.png?branch=main)](https://travis-ci.com/DSpace/DSpace)

[DSpace Documentation](https://wiki.lyrasis.org/display/DSDOC/) |
[DSpace Releases](https://github.com/DSpace/DSpace/releases) |
[DSpace Wiki](https://wiki.lyrasis.org/display/DSPACE/Home) |
[Support](https://wiki.lyrasis.org/display/DSPACE/Support)

DSpace open source software is a turnkey repository application used by more than
2,000 organizations and institutions worldwide to provide durable access to digital resources.
For more information, visit http://www.dspace.org/

***
:warning: **Work on DSpace 7 has begun on our `main` branch.** This means that there is NO user interface on this `main` branch. DSpace 7 will feature a new, unified [Angular](https://angular.io/) user interface, along with an enhanced, rebuilt REST API. The latest status of this work can be found on the [DSpace 7 Working Group](https://wiki.lyrasis.org/display/DSPACE/DSpace+7+Working+Group) page.  Additionally, the codebases can be found in the following places:
  * DSpace 7 REST API work is occurring on the [`main` branch](https://github.com/DSpace/DSpace/tree/main/dspace-server-webapp) of this repository.
     * The REST Contract is at https://github.com/DSpace/Rest7Contract
  * DSpace 7 Angular UI work is occurring at https://github.com/DSpace/dspace-angular

**If you would like to get involved in our DSpace 7 development effort, we welcome new contributors.** Just join one of our meetings or get in touch via Slack. See the [DSpace 7 Working Group](https://wiki.lyrasis.org/display/DSPACE/DSpace+7+Working+Group) wiki page for more info.

**If you are looking for the ongoing maintenance work for DSpace 6 (or prior releases)**, you can find that work on the corresponding maintenance branch (e.g. [`dspace-6_x`](https://github.com/DSpace/DSpace/tree/dspace-6_x)) in this repository.
***

## Downloads

The latest release of DSpace can be downloaded from the [DSpace website](http://www.dspace.org/latest-release/) or from [GitHub](https://github.com/DSpace/DSpace/releases).

Past releases are all available via GitHub at https://github.com/DSpace/DSpace/releases

## Documentation / Installation

Documentation for each release may be viewed online or downloaded via our [Documentation Wiki](https://wiki.lyrasis.org/display/DSDOC/).

The latest DSpace Installation instructions are available at:
https://wiki.lyrasis.org/display/DSDOC6x/Installing+DSpace

Please be aware that, as a Java web application, DSpace requires a database (PostgreSQL or Oracle)
and a servlet container (usually Tomcat) in order to function.
More information about these and all other prerequisites can be found in the Installation instructions above.

## Running DSpace 7 in Docker
See [Running DSpace 7 with Docker Compose](dspace/src/main/docker-compose/README.md)

## Contributing

DSpace is a community built and supported project. We do not have a centralized development or support team,
but have a dedicated group of volunteers who help us improve the software, documentation, resources, etc.

We welcome contributions of any type. Here's a few basic guides that provide suggestions for contributing to DSpace:
* [How to Contribute to DSpace](https://wiki.lyrasis.org/display/DSPACE/How+to+Contribute+to+DSpace): How to contribute in general (via code, documentation, bug reports, expertise, etc)
* [Code Contribution Guidelines](https://wiki.lyrasis.org/display/DSPACE/Code+Contribution+Guidelines): How to give back code or contribute features, bug fixes, etc.
* [DSpace Community Advisory Team (DCAT)](https://wiki.lyrasis.org/display/cmtygp/DSpace+Community+Advisory+Team): If you are not a developer, we also have an interest group specifically for repository managers. The DCAT group meets virtually, once a month, and sends open invitations to join their meetings via the [DCAT mailing list](https://groups.google.com/d/forum/DSpaceCommunityAdvisoryTeam).

We also encourage GitHub Pull Requests (PRs) at any time. Please see our [Development with Git](https://wiki.lyrasis.org/display/DSPACE/Development+with+Git) guide for more info.

In addition, a listing of all known contributors to DSpace software can be
found online at: https://wiki.duraspace.org/display/DSPACE/DSpaceContributors

## Getting Help

DSpace provides public mailing lists where you can post questions or raise topics for discussion.
We welcome everyone to participate in these lists:

* [dspace-community@googlegroups.com](https://groups.google.com/d/forum/dspace-community) : General discussion about DSpace platform, announcements, sharing of best practices
* [dspace-tech@googlegroups.com](https://groups.google.com/d/forum/dspace-tech) : Technical support mailing list. See also our guide for [How to troubleshoot an error](https://wiki.lyrasis.org/display/DSPACE/Troubleshoot+an+error).
* [dspace-devel@googlegroups.com](https://groups.google.com/d/forum/dspace-devel) : Developers / Development mailing list

Great Q&A is also available under the [DSpace tag on Stackoverflow](http://stackoverflow.com/questions/tagged/dspace)

Additional support options are at https://wiki.lyrasis.org/display/DSPACE/Support

DSpace also has an active service provider network. If you'd rather hire a service provider to
install, upgrade, customize or host DSpace, then we recommend getting in touch with one of our
[Registered Service Providers](http://www.dspace.org/service-providers).

## Issue Tracker

The DSpace Issue Tracker can be found at: https://jira.duraspace.org/projects/DS/summary

## Testing

### Running Tests

By default, in DSpace, Unit Tests and Integration Tests are disabled. However, they are
run automatically by [Travis CI](https://travis-ci.com/DSpace/DSpace/) for all Pull Requests and code commits.

* How to run both Unit Tests (via `maven-surefire-plugin`) and Integration Tests (via `maven-failsafe-plugin`):
  ```
  mvn clean test -Dmaven.test.skip=false -DskipITs=false
  ```
* How to run just Unit Tests:
  ```
  mvn test -Dmaven.test.skip=false
  ```
* How to run a *single* Unit Test
  ```
  # Run all tests in a specific test class
  # NOTE: failIfNoTests=false is required to skip tests in other modules
  mvn test -Dmaven.test.skip=false -Dtest=[full.package.testClassName] -DfailIfNoTests=false

  # Run one test method in a specific test class
  mvn test -Dmaven.test.skip=false -Dtest=[full.package.testClassName]#[testMethodName] -DfailIfNoTests=false
  ```
* How to run Integration Tests (requires enabling Unit tests too)
  ```
  mvn verify -Dmaven.test.skip=false -DskipITs=false
  ```
* How to run a *single* Integration Test (requires enabling Unit tests too)
  ```
  # Run all integration tests in a specific test class
  # NOTE: failIfNoTests=false is required to skip tests in other modules
  mvn test -Dmaven.test.skip=false -DskipITs=false -Dtest=[full.package.testClassName] -DfailIfNoTests=false

  # Run one test method in a specific test class
  mvn test -Dmaven.test.skip=false -DskipITs=false -Dtest=[full.package.testClassName]#[testMethodName] -DfailIfNoTests=false
  ```
* How to run only tests of a specific DSpace module
  ```
  # Before you can run only one module's tests, other modules may need installing into your ~/.m2
  cd [dspace-src]
  mvn clean install

  # Then, move into a module subdirectory, and run the test command
  cd [dspace-src]/dspace-server-webapp
  # Choose your test command from the lists above
  ```

## License

DSpace source code is freely available under a standard [BSD 3-Clause license](https://opensource.org/licenses/BSD-3-Clause).
The full license is available in the [LICENSE](LICENSE) file or online at http://www.dspace.org/license/
