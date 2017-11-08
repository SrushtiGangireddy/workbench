# coding: utf-8

"""
    AllOfUs Workbench API

    The API for the AllOfUs workbench.

    OpenAPI spec version: 0.1.0
    
    Generated by: https://github.com/swagger-api/swagger-codegen.git
"""


from pprint import pformat
from six import iteritems
import re


class SearchGroupItem(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """


    """
    Attributes:
      swagger_types (dict): The key is attribute name
                            and the value is attribute type.
      attribute_map (dict): The key is attribute name
                            and the value is json key in definition.
    """
    swagger_types = {
        'type': 'str',
        'search_parameters': 'list[SearchParameter]',
        'modifiers': 'list[Modifier]'
    }

    attribute_map = {
        'type': 'type',
        'search_parameters': 'searchParameters',
        'modifiers': 'modifiers'
    }

    def __init__(self, type=None, search_parameters=None, modifiers=None):
        """
        SearchGroupItem - a model defined in Swagger
        """

        self._type = None
        self._search_parameters = None
        self._modifiers = None
        self.discriminator = None

        self.type = type
        self.search_parameters = search_parameters
        if modifiers is not None:
          self.modifiers = modifiers

    @property
    def type(self):
        """
        Gets the type of this SearchGroupItem.
        type of criteria

        :return: The type of this SearchGroupItem.
        :rtype: str
        """
        return self._type

    @type.setter
    def type(self, type):
        """
        Sets the type of this SearchGroupItem.
        type of criteria

        :param type: The type of this SearchGroupItem.
        :type: str
        """
        if type is None:
            raise ValueError("Invalid value for `type`, must not be `None`")

        self._type = type

    @property
    def search_parameters(self):
        """
        Gets the search_parameters of this SearchGroupItem.
        values that help search for subjects

        :return: The search_parameters of this SearchGroupItem.
        :rtype: list[SearchParameter]
        """
        return self._search_parameters

    @search_parameters.setter
    def search_parameters(self, search_parameters):
        """
        Sets the search_parameters of this SearchGroupItem.
        values that help search for subjects

        :param search_parameters: The search_parameters of this SearchGroupItem.
        :type: list[SearchParameter]
        """
        if search_parameters is None:
            raise ValueError("Invalid value for `search_parameters`, must not be `None`")

        self._search_parameters = search_parameters

    @property
    def modifiers(self):
        """
        Gets the modifiers of this SearchGroupItem.
        criteria by operation or predicate

        :return: The modifiers of this SearchGroupItem.
        :rtype: list[Modifier]
        """
        return self._modifiers

    @modifiers.setter
    def modifiers(self, modifiers):
        """
        Sets the modifiers of this SearchGroupItem.
        criteria by operation or predicate

        :param modifiers: The modifiers of this SearchGroupItem.
        :type: list[Modifier]
        """

        self._modifiers = modifiers

    def to_dict(self):
        """
        Returns the model properties as a dict
        """
        result = {}

        for attr, _ in iteritems(self.swagger_types):
            value = getattr(self, attr)
            if isinstance(value, list):
                result[attr] = list(map(
                    lambda x: x.to_dict() if hasattr(x, "to_dict") else x,
                    value
                ))
            elif hasattr(value, "to_dict"):
                result[attr] = value.to_dict()
            elif isinstance(value, dict):
                result[attr] = dict(map(
                    lambda item: (item[0], item[1].to_dict())
                    if hasattr(item[1], "to_dict") else item,
                    value.items()
                ))
            else:
                result[attr] = value

        return result

    def to_str(self):
        """
        Returns the string representation of the model
        """
        return pformat(self.to_dict())

    def __repr__(self):
        """
        For `print` and `pprint`
        """
        return self.to_str()

    def __eq__(self, other):
        """
        Returns true if both objects are equal
        """
        if not isinstance(other, SearchGroupItem):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
